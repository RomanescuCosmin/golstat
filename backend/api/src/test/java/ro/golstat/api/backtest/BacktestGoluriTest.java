package ro.golstat.api.backtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ro.golstat.stats.goals.GoalForm;
import ro.golstat.stats.match.MatchContext;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.match.MatchPrediction;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.math.ScoreGrid;
import ro.golstat.stats.model.MatchSample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Backtest doar pentru GOLURI, pe regula ferestrelor de 7 (gazdele ultimele 7 acasa, oaspetii ultimele 7
 * in deplasare). Compara modelul actual cu variantele cerute de utilizator: doar golurile celor doua
 * echipe (fara media ligii), plus forta adversarilor si ponderarea pe recenta. Fiecare meci e prezis
 * DOAR din meciurile dinaintea lui. Ruleaza cu {@code GOLSTAT_BACKTEST=1}.
 */
@EnabledIfEnvironmentVariable(named = "GOLSTAT_BACKTEST", matches = "1")
class BacktestGoluriTest {

    private static final int FEREASTRA = 7;
    private static final int MIN_FEREASTRA = 3;
    private static final int MIN_MECIURI_LIGA = 20;
    /** Cate meciuri trebuie sa aiba un adversar ca sa-i credem pe deplin forta; sub asta e tras spre 1. */
    private static final double K_ADVERSAR = 5;
    private static final double RECENTA = 0.85;

    /** Un meci din perspectiva unei echipe. */
    record Meci(LocalDateTime kickoff, boolean acasa, int marcate, int primite, long adversar) {
    }

    record Rand(long id, long liga, LocalDateTime kickoff, long gazde, long oaspeti, int gG, int gO) {
    }

    /** Media curenta de goluri marcate / primite ale unei echipe (toate meciurile ei anterioare). */
    static final class Forta {
        double marcate;
        double primite;
        int n;
    }

    static final class Acc {
        double brier;
        double pred;
        double actual;
        int n;
        final TreeMap<Integer, double[]> calibrare = new TreeMap<>();

        void add(double p, boolean hit) {
            double y = hit ? 1 : 0;
            brier += (p - y) * (p - y);
            pred += p;
            actual += y;
            n++;
            double[] b = calibrare.computeIfAbsent((int) Math.min(9, Math.floor(p * 10)), k -> new double[3]);
            b[0] += p;
            b[1] += y;
            b[2]++;
        }
    }

    private final Map<String, Acc> rezultate = new LinkedHashMap<>();

    @Test
    void backtest() throws Exception {
        List<Rand> meciuri = incarca();
        Map<Long, List<Meci>> istoric = new HashMap<>();
        Map<Long, Forta> forte = new HashMap<>();
        Map<Long, double[]> ligi = new HashMap<>();
        double[] global = new double[2];

        for (Rand r : meciuri) {
            double[] l = ligi.computeIfAbsent(r.liga(), k -> new double[3]);
            List<Meci> gazde = istoric.getOrDefault(r.gazde(), List.of());
            List<Meci> oaspeti = istoric.getOrDefault(r.oaspeti(), List.of());
            List<Meci> gLoc = ultimele(gazde.stream().filter(Meci::acasa).toList());
            List<Meci> oLoc = ultimele(oaspeti.stream().filter(m -> !m.acasa()).toList());

            if (l[2] >= MIN_MECIURI_LIGA && gLoc.size() >= MIN_FEREASTRA && oLoc.size() >= MIN_FEREASTRA) {
                double medieGolEchipa = global[1] > 0 ? global[0] / global[1] : 1.35;
                evalueaza(r, gLoc, oLoc, l[0] / l[2], l[1] / l[2], forte, medieGolEchipa);
            }

            l[0] += r.gG();
            l[1] += r.gO();
            l[2]++;
            global[0] += r.gG() + r.gO();
            global[1] += 2;
            istoric.computeIfAbsent(r.gazde(), k -> new ArrayList<>())
                    .add(new Meci(r.kickoff(), true, r.gG(), r.gO(), r.oaspeti()));
            istoric.computeIfAbsent(r.oaspeti(), k -> new ArrayList<>())
                    .add(new Meci(r.kickoff(), false, r.gO(), r.gG(), r.gazde()));
            actualizeaza(forte, r.gazde(), r.gG(), r.gO());
            actualizeaza(forte, r.oaspeti(), r.gO(), r.gG());
        }
        scrieRaport(meciuri.size());
    }

    private void evalueaza(Rand r, List<Meci> gLoc, List<Meci> oLoc, double mg, double mo,
                           Map<Long, Forta> forte, double medie) {
        Map<String, double[]> variante = new LinkedHashMap<>();
        MatchPrediction actual = MatchGoalModel.predict(new MatchContext(
                GoalForm.of(samples(gLoc)), GoalForm.of(samples(oLoc)), mg, mo));
        variante.put("A actual (media ligii, K=3)", new double[]{actual.lambdaGazde(), actual.lambdaOaspeti()});
        variante.put("B doar echipele", doarEchipele(gLoc, oLoc, false, false, forte, medie));
        variante.put("C echipe + forta adversari", doarEchipele(gLoc, oLoc, true, false, forte, medie));
        variante.put("D echipe + recenta", doarEchipele(gLoc, oLoc, false, true, forte, medie));
        variante.put("E echipe + forta + recenta", doarEchipele(gLoc, oLoc, true, true, forte, medie));

        int total = r.gG() + r.gO();
        boolean gg = r.gG() > 0 && r.gO() > 0;
        acc("goluri total 1.5", "0 rata-ligii").add(Poisson.probabilityOver(mg + mo, 1.5), total > 1.5);
        acc("goluri total 2.5", "0 rata-ligii").add(Poisson.probabilityOver(mg + mo, 2.5), total > 2.5);
        acc("goluri total 3.5", "0 rata-ligii").add(Poisson.probabilityOver(mg + mo, 3.5), total > 3.5);
        acc("goluri GG", "0 rata-ligii").add((1 - Math.exp(-mg)) * (1 - Math.exp(-mo)), gg);
        acc("1X2 gazde", "0 rata-ligii").add(0.45, r.gG() > r.gO());
        acc("1X2 egal", "0 rata-ligii").add(0.25, r.gG() == r.gO());
        acc("1X2 oaspeti", "0 rata-ligii").add(0.30, r.gG() < r.gO());
        acc("goluri gazde 0.5", "0 rata-ligii").add(Poisson.probabilityOver(mg, 0.5), r.gG() > 0.5);
        acc("goluri gazde 1.5", "0 rata-ligii").add(Poisson.probabilityOver(mg, 1.5), r.gG() > 1.5);
        acc("goluri oaspeti 0.5", "0 rata-ligii").add(Poisson.probabilityOver(mo, 0.5), r.gO() > 0.5);
        acc("goluri oaspeti 1.5", "0 rata-ligii").add(Poisson.probabilityOver(mo, 1.5), r.gO() > 1.5);

        variante.forEach((nume, l) -> {
            ScoreGrid grid = ScoreGrid.dixonColes(Math.max(l[0], 0.05), Math.max(l[1], 0.05),
                    MatchGoalModel.RHO, MatchGoalModel.MAX_GOALS);
            for (double linie : new double[]{1.5, 2.5, 3.5}) {
                acc("goluri total " + linie, nume).add(grid.probabilityOverTotal(linie), total > linie);
            }
            acc("goluri GG", nume).add(grid.btts(), gg);
            acc("1X2 gazde", nume).add(grid.homeWin(), r.gG() > r.gO());
            acc("1X2 egal", nume).add(grid.draw(), r.gG() == r.gO());
            acc("1X2 oaspeti", nume).add(grid.awayWin(), r.gG() < r.gO());
            for (double linie : new double[]{0.5, 1.5}) {
                acc("goluri gazde " + linie, nume).add(Poisson.probabilityOver(l[0], linie), r.gG() > linie);
                acc("goluri oaspeti " + linie, nume).add(Poisson.probabilityOver(l[1], linie), r.gO() > linie);
            }
        });
    }

    /**
     * λ fara media ligii: λ gazde = (marcate de gazde acasa + primite de oaspeti in deplasare) / 2.
     * {@code forta}: fiecare gol se cantareste dupa adversar — marcat contra unei aparari care primeste
     * putin valoreaza mai mult; {@code recenta}: meciurile noi cantaresc mai mult (0.85 pe meci).
     */
    private static double[] doarEchipele(List<Meci> gLoc, List<Meci> oLoc, boolean forta, boolean recenta,
                                         Map<Long, Forta> forte, double medie) {
        double gMarcate = medie(gLoc, true, forta, recenta, forte, medie);
        double gPrimite = medie(gLoc, false, forta, recenta, forte, medie);
        double oMarcate = medie(oLoc, true, forta, recenta, forte, medie);
        double oPrimite = medie(oLoc, false, forta, recenta, forte, medie);
        return new double[]{(gMarcate + oPrimite) / 2, (oMarcate + gPrimite) / 2};
    }

    private static double medie(List<Meci> fereastra, boolean marcate, boolean forta, boolean recenta,
                                Map<Long, Forta> forte, double medie) {
        double suma = 0;
        double ponderi = 0;
        for (int i = 0; i < fereastra.size(); i++) {
            Meci m = fereastra.get(i);
            double valoare = marcate ? m.marcate() : m.primite();
            if (forta) {
                Forta a = forte.get(m.adversar());
                if (a != null && a.n > 0) {
                    // marcate: impartim la cat de permeabil e adversarul; primite: la cat de bine ataca
                    double adv = marcate ? a.primite / a.n : a.marcate / a.n;
                    double raport = (a.n * (medie / Math.max(adv, 0.2)) + K_ADVERSAR) / (a.n + K_ADVERSAR);
                    valoare *= raport;
                }
            }
            double w = recenta ? Math.pow(RECENTA, fereastra.size() - 1 - i) : 1;
            suma += w * valoare;
            ponderi += w;
        }
        return ponderi > 0 ? suma / ponderi : medie;
    }

    private static void actualizeaza(Map<Long, Forta> forte, long echipa, int marcate, int primite) {
        Forta f = forte.computeIfAbsent(echipa, k -> new Forta());
        f.marcate += marcate;
        f.primite += primite;
        f.n++;
    }

    private static List<MatchSample> samples(List<Meci> fereastra) {
        return fereastra.stream()
                .map(m -> new MatchSample(m.kickoff().toLocalDate(), m.acasa(), m.marcate(), m.primite(), 0, 0, null))
                .toList();
    }

    private static List<Meci> ultimele(List<Meci> lista) {
        return lista.subList(Math.max(0, lista.size() - FEREASTRA), lista.size());
    }

    private Acc acc(String piata, String varianta) {
        return rezultate.computeIfAbsent(piata + " | " + varianta, k -> new Acc());
    }

    private static List<Rand> incarca() throws Exception {
        String url = System.getenv().getOrDefault("GOLSTAT_BACKTEST_URL",
                "jdbc:mysql://localhost:3306/golstat_bzz?serverTimezone=UTC");
        String sql = """
                select f.id, f.league_id, f.kickoff, f.home_team_id, f.away_team_id,
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
                out.add(new Rand(rs.getLong(1), rs.getLong(2), rs.getTimestamp(3).toLocalDateTime(),
                        rs.getLong(4), rs.getLong(5), rs.getInt(6), rs.getInt(7)));
            }
        }
        return out;
    }

    private void scrieRaport(int meciuri) throws Exception {
        StringBuilder sb = new StringBuilder("Backtest goluri pe " + meciuri + " meciuri terminate (ferestre de 7)\n");
        sb.append(String.format("%-20s %-30s %5s %7s %7s %8s %7s%n",
                "piata", "varianta", "n", "prezis", "real", "brier", "skill"));
        Map<String, Double> baza = new HashMap<>();
        rezultate.forEach((cheie, a) -> {
            String[] p = cheie.split(" \\| ");
            if (p[1].startsWith("0 ")) {
                baza.put(p[0], a.brier / a.n);
            }
        });
        rezultate.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            String[] p = e.getKey().split(" \\| ");
            Acc a = e.getValue();
            double brier = a.brier / a.n;
            double b = baza.getOrDefault(p[0], brier);
            sb.append(String.format("%-20s %-30s %5d %6.1f%% %6.1f%% %8.4f %+6.1f%%%n",
                    p[0], p[1], a.n, 100 * a.pred / a.n, 100 * a.actual / a.n, brier, 100 * (1 - brier / b)));
        });
        sb.append("\nCalibrare peste 2.5 goluri (cand modelul zice X%, cat de des s-a intamplat):\n");
        rezultate.forEach((cheie, a) -> {
            if (!cheie.startsWith("goluri total 2.5 |")) {
                return;
            }
            sb.append(cheie.substring("goluri total 2.5 | ".length())).append(":");
            a.calibrare.forEach((bin, v) -> sb.append(String.format("  %2.0f%%→%2.0f%% (n=%d)",
                    100 * v[0] / v[2], 100 * v[1] / v[2], (int) v[2])));
            sb.append('\n');
        });
        Path out = Path.of(System.getenv().getOrDefault("GOLSTAT_BACKTEST_OUT", "build/backtest-goluri.txt"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }
}
