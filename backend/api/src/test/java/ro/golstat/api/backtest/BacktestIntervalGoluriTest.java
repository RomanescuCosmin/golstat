package ro.golstat.api.backtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ro.golstat.stats.goals.GoalForm;
import ro.golstat.stats.match.MatchContext;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.match.MatchPrediction;
import ro.golstat.stats.model.MatchSample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Backtest pentru pietele „interval de goluri” (intre a si b goluri), pe modelul din productie
 * ({@link MatchGoalModel}, ferestrele de 7 acasa / in deplasare). Fiecare meci e prezis doar din meciurile
 * dinaintea lui. Compara cu rata ligii pana in acel moment (Brier skill) si arata calibrarea.
 * Ruleaza cu {@code GOLSTAT_BACKTEST=1}; raportul: {@code build/backtest-interval-goluri.txt}.
 */
@EnabledIfEnvironmentVariable(named = "GOLSTAT_BACKTEST", matches = "1")
class BacktestIntervalGoluriTest {

    private static final int FEREASTRA = 7;
    private static final int MIN_FEREASTRA = 3;
    private static final int MIN_MECIURI_LIGA = 20;
    private static final int MAXIM = 6;
    private static final int[][] INTERVALE = {
            {0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 3}, {2, 4}, {2, 5}, {3, 4}, {3, 5}, {3, 6}, {4, 6}};

    record Rand(long liga, LocalDate zi, long gazde, long oaspeti, int gG, int gO) {
    }

    record Meci(LocalDate zi, boolean acasa, int marcate, int primite) {
    }

    static final class Acc {
        double brierModel;
        double brierLiga;
        double pred;
        double real;
        int n;
        final TreeMap<Integer, double[]> calibrare = new TreeMap<>();

        void add(double p, double pLiga, boolean hit) {
            double y = hit ? 1 : 0;
            brierModel += (p - y) * (p - y);
            brierLiga += (pLiga - y) * (pLiga - y);
            pred += p;
            real += y;
            n++;
            double[] b = calibrare.computeIfAbsent((int) Math.min(9, Math.floor(p * 10)), k -> new double[3]);
            b[0] += p;
            b[1] += y;
            b[2]++;
        }
    }

    @Test
    void backtest() throws Exception {
        List<Rand> meciuri = incarca();
        Map<Long, List<Meci>> istoric = new HashMap<>();
        Map<Long, double[]> ligi = new HashMap<>();          // {goluri gazde, goluri oaspeti, n}
        Map<Long, int[]> totaluriLiga = new HashMap<>();     // [k] = cate meciuri cu k goluri (ultimul = 7+)
        Map<String, Acc> rezultate = new LinkedHashMap<>();
        for (double w : PONDERI_LIGA) {
            for (int[] iv : INTERVALE) {
                rezultate.put(cheie(iv, w), new Acc());
            }
        }

        for (Rand r : meciuri) {
            double[] l = ligi.computeIfAbsent(r.liga(), k -> new double[3]);
            int[] tl = totaluriLiga.computeIfAbsent(r.liga(), k -> new int[MAXIM + 2]);
            List<Meci> gLoc = ultimele(istoric.getOrDefault(r.gazde(), List.of()).stream().filter(Meci::acasa).toList());
            List<Meci> oLoc = ultimele(istoric.getOrDefault(r.oaspeti(), List.of()).stream().filter(m -> !m.acasa()).toList());

            if (l[2] >= MIN_MECIURI_LIGA && gLoc.size() >= MIN_FEREASTRA && oLoc.size() >= MIN_FEREASTRA) {
                MatchPrediction p = MatchGoalModel.predict(new MatchContext(
                        GoalForm.of(samples(gLoc)), GoalForm.of(samples(oLoc)), l[0] / l[2], l[1] / l[2]));
                double[] d = MatchGoalModel.distributieTotal(p, MAXIM);
                int total = r.gG() + r.gO();
                for (int[] iv : INTERVALE) {
                    double pModel = suma(d, iv);
                    double pLiga = rataLiga(tl, iv, l[2]);
                    for (double w : PONDERI_LIGA) {
                        rezultate.get(cheie(iv, w)).add((1 - w) * pModel + w * pLiga, pLiga,
                                total >= iv[0] && total <= iv[1]);
                    }
                }
            }

            l[0] += r.gG();
            l[1] += r.gO();
            l[2]++;
            tl[Math.min(r.gG() + r.gO(), MAXIM + 1)]++;
            istoric.computeIfAbsent(r.gazde(), k -> new ArrayList<>()).add(new Meci(r.zi(), true, r.gG(), r.gO()));
            istoric.computeIfAbsent(r.oaspeti(), k -> new ArrayList<>()).add(new Meci(r.zi(), false, r.gO(), r.gG()));
        }
        scrieRaport(meciuri.size(), rezultate);
    }

    private static double suma(double[] d, int[] iv) {
        double s = 0;
        for (int k = iv[0]; k <= iv[1]; k++) {
            s += d[k];
        }
        return s;
    }

    private static double rataLiga(int[] tl, int[] iv, double n) {
        int s = 0;
        for (int k = iv[0]; k <= iv[1]; k++) {
            s += tl[k];
        }
        return s / n;
    }

    /** Ponderea ratei ligii in amestec: 0 = doar modelul, 0.5 = jumatate-jumatate. */
    private static final double[] PONDERI_LIGA = {0.0, 0.3, 0.5, 0.7};

    private static String cheie(int[] iv, double w) {
        return String.format("%d-%d %s", iv[0], iv[1], w == 0 ? "model" : "liga " + Math.round(w * 100) + "%");
    }

    private static List<Meci> ultimele(List<Meci> lista) {
        return lista.subList(Math.max(0, lista.size() - FEREASTRA), lista.size());
    }

    private static List<MatchSample> samples(List<Meci> fereastra) {
        return fereastra.stream()
                .map(m -> new MatchSample(m.zi(), m.acasa(), m.marcate(), m.primite(), 0, 0, null))
                .toList();
    }

    private static List<Rand> incarca() throws Exception {
        String url = System.getenv().getOrDefault("GOLSTAT_BACKTEST_URL",
                "jdbc:mysql://localhost:3306/golstat_bzz?serverTimezone=UTC");
        String sql = """
                select f.league_id, f.kickoff, f.home_team_id, f.away_team_id,
                       coalesce(f.score_ft_home, f.goals_home), coalesce(f.score_ft_away, f.goals_away)
                from fixture f
                where f.status_short in ('FT', 'AET', 'PEN') and f.kickoff is not null
                order by f.kickoff, f.id
                """;
        List<Rand> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, "golstat", "golstat");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Rand(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime().toLocalDate(),
                        rs.getLong(3), rs.getLong(4), rs.getInt(5), rs.getInt(6)));
            }
        }
        return out;
    }

    /**
     * Pe interval: n, prezis vs real (media), abaterea in pp, Brier model vs rata ligii si skill-ul;
     * calibrarea = cand modelul zice X%, cat de des s-a intamplat (doar intervalele cu n ≥ 50).
     */
    private static void scrieRaport(int meciuri, Map<String, Acc> rezultate) throws Exception {
        StringBuilder sb = new StringBuilder("Backtest interval goluri pe " + meciuri
                + " meciuri terminate (MatchGoalModel, ferestre de 7)\n\n");
        // sumar pe varianta: media pe toate intervalele (abatere absoluta, skill) si cea mai mare abatere de calibrare
        sb.append(String.format("%-14s %10s %8s %12s%n", "varianta", "|abatere|", "skill", "max calibr."));
        for (double w : PONDERI_LIGA) {
            double abatere = 0;
            double skill = 0;
            double calibrare = 0;
            for (int[] iv : INTERVALE) {
                Acc a = rezultate.get(cheie(iv, w));
                abatere += Math.abs(100 * (a.pred - a.real) / a.n);
                skill += 100 * (1 - a.brierModel / a.brierLiga);
                for (double[] b : a.calibrare.values()) {
                    if (b[2] >= 50) {
                        calibrare = Math.max(calibrare, 100 * Math.abs(b[0] - b[1]) / b[2]);
                    }
                }
            }
            sb.append(String.format("%-14s %8.1fpp %+7.1f%% %10.1fpp%n", w == 0 ? "model" : "liga " + Math.round(w * 100) + "%",
                    abatere / INTERVALE.length, skill / INTERVALE.length, calibrare));
        }
        sb.append('\n');
        sb.append(String.format("%-12s %6s %7s %7s %7s %8s %8s %7s%n",
                "interval", "n", "prezis", "real", "abatere", "brier", "b.liga", "skill"));
        rezultate.forEach((nume, a) -> sb.append(String.format("%-12s %6d %6.1f%% %6.1f%% %+6.1fpp %8.4f %8.4f %+6.1f%%%n",
                nume, a.n, 100 * a.pred / a.n, 100 * a.real / a.n, 100 * (a.pred - a.real) / a.n,
                a.brierModel / a.n, a.brierLiga / a.n, 100 * (1 - a.brierModel / a.brierLiga))));
        sb.append("\nCalibrare (prezis → real, n):\n");
        rezultate.forEach((nume, a) -> {
            sb.append(String.format("%-12s", nume));
            a.calibrare.forEach((bin, v) -> {
                if (v[2] >= 50) {
                    sb.append(String.format("  %2.0f%%→%2.0f%% (%d)", 100 * v[0] / v[2], 100 * v[1] / v[2], (int) v[2]));
                }
            });
            sb.append('\n');
        });
        Path out = Path.of(System.getenv().getOrDefault("GOLSTAT_BACKTEST_INTERVAL_OUT",
                "build/backtest-interval-goluri.txt"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }
}
