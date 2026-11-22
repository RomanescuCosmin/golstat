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
import java.util.ArrayDeque;
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
    private static final double RHO = MatchGoalModel.RHO;
    private static final double[] FACTORI = {1.03, 1.05, 1.08};
    private static final int[] N_RECENTE = {50, 100, 200};
    private static final String[] CALIBRATE = {"goluri total 1.5", "goluri total 2.5", "goluri GG"};

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
        Map<Long, ArrayDeque<int[]>> recente = new HashMap<>();
        Map<Long, Map<String, double[]>> numarLiga = new HashMap<>();
        double[] global = new double[2];

        for (Rand r : meciuri) {
            double[] l = ligi.computeIfAbsent(r.liga(), k -> new double[3]);
            ArrayDeque<int[]> rec = recente.computeIfAbsent(r.liga(), k -> new ArrayDeque<>());
            Map<String, double[]> nl = numarLiga.computeIfAbsent(r.liga(), k -> new LinkedHashMap<>());
            List<Meci> gazde = istoric.getOrDefault(r.gazde(), List.of());
            List<Meci> oaspeti = istoric.getOrDefault(r.oaspeti(), List.of());
            List<Meci> gLoc = ultimele(gazde.stream().filter(Meci::acasa).toList());
            List<Meci> oLoc = ultimele(oaspeti.stream().filter(m -> !m.acasa()).toList());

            if (l[2] >= MIN_MECIURI_LIGA && gLoc.size() >= MIN_FEREASTRA && oLoc.size() >= MIN_FEREASTRA) {
                double medieGolEchipa = global[1] > 0 ? global[0] / global[1] : 1.35;
                evalueaza(r, gLoc, oLoc, l[0] / l[2], l[1] / l[2], forte, medieGolEchipa, mediiRecente(rec), rate(nl));
            }

            l[0] += r.gG();
            l[1] += r.gO();
            l[2]++;
            numara(nl, evenimente(r.gG(), r.gO()));
            rec.addFirst(new int[]{r.gG(), r.gO()});
            if (rec.size() > N_RECENTE[N_RECENTE.length - 1]) {
                rec.removeLast();
            }
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
                           Map<Long, Forta> forte, double medie, Map<Integer, double[]> recente,
                           Map<String, Double> rateLiga) {
        // {λ gazde, λ oaspeti, rho}
        Map<String, double[]> variante = new LinkedHashMap<>();
        MatchPrediction actual = MatchGoalModel.predict(new MatchContext(
                GoalForm.of(samples(gLoc)), GoalForm.of(samples(oLoc)), mg, mo));
        // MatchGoalModel nu foloseste media ligii cand ferestrele au meciuri → identic cu B
        variante.put("A MatchGoalModel (productie)", new double[]{actual.lambdaGazde(), actual.lambdaOaspeti(), RHO});
        variante.put("B doar echipele", cuRho(doarEchipele(gLoc, oLoc, false, false, forte, medie), RHO));
        variante.put("C echipe + forta adversari", cuRho(doarEchipele(gLoc, oLoc, true, false, forte, medie), RHO));
        variante.put("D echipe + recenta", cuRho(doarEchipele(gLoc, oLoc, false, true, forte, medie), RHO));
        variante.put("E echipe + forta + recenta", cuRho(doarEchipele(gLoc, oLoc, true, true, forte, medie), RHO));

        // pasul 1c: forma distributiei, pe baza K + forta + λ ×1.03 + rho +0.05
        double n = (gLoc.size() + oLoc.size()) / 2.0;
        double[] echipe = doarEchipele(gLoc, oLoc, true, false, forte, medie);
        Map<String, Map<String, Double>> probabilitati = new LinkedHashMap<>();
        variante.forEach((nume, l) -> probabilitati.put(nume, dinGrila(l)));
        Map<String, Double> ratEchipe = rateEchipe(gLoc, oLoc);
        probabilitati.put("0b liga empiric", rateLiga);
        for (int k : new int[]{7, 10, 15}) {
            double[] l = amesteca(echipe, mg, mo, n / (n + k), 0.05);
            Map<String, Double> baza = dinGrila(new double[]{1.03 * l[0], 1.03 * l[1], 0.05});
            String nume = "K" + k + " x1.03";
            probabilitati.put(nume, baza);
            for (double a : new double[]{0.2, 0.35, 0.5}) {
                probabilitati.put(String.format("%s +liga %.2f", nume, a), amestecPeLinie(baza, rateLiga, a));
            }
            for (double b : new double[]{0.2, 0.35}) {
                probabilitati.put(String.format("%s +echipe %.2f", nume, b), amestecPeLinie(baza, ratEchipe, b));
            }
            Map<String, Double> faraFactor = dinGrila(l);
            for (double a : new double[]{0.35, 0.5}) {
                probabilitati.put(String.format("K%d +liga %.2f", k, a), amestecPeLinie(faraFactor, rateLiga, a));
            }
        }
        diagnostic("liga cumulata", mg + mo, r);
        diagnostic("echipe +forta", echipe[0] + echipe[1], r);

        acc("goluri total 1.5", "0 rata-ligii").add(Poisson.probabilityOver(mg + mo, 1.5), r.gG() + r.gO() > 1.5);
        acc("goluri total 2.5", "0 rata-ligii").add(Poisson.probabilityOver(mg + mo, 2.5), r.gG() + r.gO() > 2.5);
        acc("goluri total 3.5", "0 rata-ligii").add(Poisson.probabilityOver(mg + mo, 3.5), r.gG() + r.gO() > 3.5);
        acc("goluri GG", "0 rata-ligii").add((1 - Math.exp(-mg)) * (1 - Math.exp(-mo)), r.gG() > 0 && r.gO() > 0);
        acc("1X2 gazde", "0 rata-ligii").add(0.45, r.gG() > r.gO());
        acc("1X2 egal", "0 rata-ligii").add(0.25, r.gG() == r.gO());
        acc("1X2 oaspeti", "0 rata-ligii").add(0.30, r.gG() < r.gO());
        acc("goluri gazde 0.5", "0 rata-ligii").add(Poisson.probabilityOver(mg, 0.5), r.gG() > 0.5);
        acc("goluri gazde 1.5", "0 rata-ligii").add(Poisson.probabilityOver(mg, 1.5), r.gG() > 1.5);
        acc("goluri oaspeti 0.5", "0 rata-ligii").add(Poisson.probabilityOver(mo, 0.5), r.gO() > 0.5);
        acc("goluri oaspeti 1.5", "0 rata-ligii").add(Poisson.probabilityOver(mo, 1.5), r.gO() > 1.5);

        Map<String, Boolean> real = evenimente(r.gG(), r.gO());
        probabilitati.forEach((nume, p) -> p.forEach((piata, prob) -> acc(piata, nume).add(prob, real.get(piata))));
    }

    /** Ce s-a intamplat intr-un meci, pe fiecare piata urmarita (scor gazde - oaspeti). */
    static Map<String, Boolean> evenimente(int h, int a) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (double linie : new double[]{1.5, 2.5, 3.5}) {
            out.put("goluri total " + linie, h + a > linie);
        }
        out.put("goluri GG", h > 0 && a > 0);
        out.put("1X2 gazde", h > a);
        out.put("1X2 egal", h == a);
        out.put("1X2 oaspeti", h < a);
        for (double linie : new double[]{0.5, 1.5}) {
            out.put("goluri gazde " + linie, h > linie);
            out.put("goluri oaspeti " + linie, a > linie);
        }
        return out;
    }

    private static Map<String, Double> dinGrila(double[] l) {
        ScoreGrid grid = ScoreGrid.dixonColes(Math.max(l[0], 0.05), Math.max(l[1], 0.05), l[2], MatchGoalModel.MAX_GOALS);
        Map<String, Double> out = new LinkedHashMap<>();
        for (double linie : new double[]{1.5, 2.5, 3.5}) {
            out.put("goluri total " + linie, grid.probabilityOverTotal(linie));
        }
        out.put("goluri GG", grid.btts());
        out.put("1X2 gazde", grid.homeWin());
        out.put("1X2 egal", grid.draw());
        out.put("1X2 oaspeti", grid.awayWin());
        for (double linie : new double[]{0.5, 1.5}) {
            out.put("goluri gazde " + linie, Poisson.probabilityOver(Math.max(l[0], 0.05), linie));
            out.put("goluri oaspeti " + linie, Poisson.probabilityOver(Math.max(l[1], 0.05), linie));
        }
        return out;
    }

    /** p = (1-a)·model + a·rata empirica, pe fiecare piata separat (corecteaza forma, nu doar nivelul). */
    private static Map<String, Double> amestecPeLinie(Map<String, Double> model, Map<String, Double> empiric, double a) {
        Map<String, Double> out = new LinkedHashMap<>();
        model.forEach((piata, p) -> out.put(piata, (1 - a) * p + a * empiric.getOrDefault(piata, p)));
        return out;
    }

    /** Ratele din ferestrele celor doua echipe, fiecare meci vazut ca gazde - oaspeti. */
    private static Map<String, Double> rateEchipe(List<Meci> gLoc, List<Meci> oLoc) {
        Map<String, double[]> numar = new LinkedHashMap<>();
        gLoc.forEach(m -> numara(numar, evenimente(m.marcate(), m.primite())));
        oLoc.forEach(m -> numara(numar, evenimente(m.primite(), m.marcate())));
        return rate(numar);
    }

    static void numara(Map<String, double[]> numar, Map<String, Boolean> ev) {
        ev.forEach((piata, hit) -> {
            double[] c = numar.computeIfAbsent(piata, k -> new double[2]);
            c[0] += hit ? 1 : 0;
            c[1]++;
        });
    }

    static Map<String, Double> rate(Map<String, double[]> numar) {
        Map<String, Double> out = new LinkedHashMap<>();
        numar.forEach((piata, c) -> out.put(piata, c[0] / c[1]));
        return out;
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

    /** Media ligii pe ultimele N meciuri (cele mai noi primele); sub N meciuri, cate sunt. */
    private static Map<Integer, double[]> mediiRecente(ArrayDeque<int[]> rec) {
        Map<Integer, double[]> out = new HashMap<>();
        for (int nr : N_RECENTE) {
            double g = 0;
            double o = 0;
            int i = 0;
            for (int[] m : rec) {
                if (i == nr) {
                    break;
                }
                g += m[0];
                o += m[1];
                i++;
            }
            out.put(nr, i > 0 ? new double[]{g / i, o / i} : new double[]{1.5, 1.2});
        }
        return out;
    }

    /** Goluri asteptate vs reale pe meci: arata daca media e decalata sau forma Poisson e de vina. */
    private final Map<String, double[]> diagnostice = new LinkedHashMap<>();

    private void diagnostic(String nume, double asteptat, Rand r) {
        double[] d = diagnostice.computeIfAbsent(nume, k -> new double[3]);
        d[0] += asteptat;
        d[1] += r.gG() + r.gO();
        d[2]++;
    }

    /** Shrinkage spre media ligii pe locatie: λ = w·λ echipa + (1-w)·λ liga. */
    private static double[] amesteca(double[] echipe, double mg, double mo, double w, double rho) {
        return new double[]{w * echipe[0] + (1 - w) * mg, w * echipe[1] + (1 - w) * mo, rho};
    }

    private static double[] cuRho(double[] l, double rho) {
        return new double[]{l[0], l[1], rho};
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

    private static final String[] PIETE_SUMAR = {"goluri total 1.5", "goluri total 2.5", "goluri total 3.5", "goluri GG"};

    /**
     * O linie pe varianta, cu criteriile de acceptare: abaterea mediei (prezis - real, pp) si skill-ul Brier
     * pe 1.5 / 2.5 / 3.5 / GG, abaterea la egal si cea mai mare abatere de calibrare la peste 2.5
     * (doar intervalele cu n ≥ 100). OK = toate criteriile indeplinite.
     */
    private String sumar(Map<String, Double> baza) {
        StringBuilder sb = new StringBuilder("SUMAR (abatere = prezis - real, pp; skill vs 0 rata-ligii)\n");
        sb.append(String.format("%-28s %6s %6s %6s %6s | %6s %6s %6s %6s | %6s | %6s %s%n", "varianta",
                "d1.5", "d2.5", "d3.5", "dGG", "s1.5", "s2.5", "s3.5", "sGG", "dEgal", "cal2.5", ""));
        rezultate.keySet().stream().map(k -> k.split(" \\| ")[1]).distinct()
                .filter(v -> !v.startsWith("0 ")).forEach(v -> {
                    boolean ok = true;
                    StringBuilder d = new StringBuilder();
                    StringBuilder s = new StringBuilder();
                    for (String p : PIETE_SUMAR) {
                        Acc a = rezultate.get(p + " | " + v);
                        double abatere = 100 * (a.pred - a.actual) / a.n;
                        double skill = 100 * (1 - (a.brier / a.n) / baza.get(p));
                        ok &= Math.abs(abatere) <= 1 && skill >= 0;
                        d.append(String.format(" %+6.1f", abatere));
                        s.append(String.format(" %+6.1f", skill));
                    }
                    Acc egal = rezultate.get("1X2 egal | " + v);
                    double dEgal = 100 * (egal.pred - egal.actual) / egal.n;
                    double cal = rezultate.get("goluri total 2.5 | " + v).calibrare.values().stream()
                            .filter(b -> b[2] >= 100)
                            .mapToDouble(b -> 100 * Math.abs(b[0] - b[1]) / b[2]).max().orElse(0);
                    ok &= Math.abs(dEgal) <= 1.5 && cal <= 5;
                    sb.append(String.format("%-28s%s |%s | %+6.1f | %6.1f %s%n", v, d, s, dEgal, cal, ok ? "OK" : ""));
                });
        return sb.append('\n').toString();
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
        for (String piata : CALIBRATE) {
            sb.append("\nCalibrare ").append(piata).append(" (cand modelul zice X%, cat de des s-a intamplat):\n");
            rezultate.forEach((cheie, a) -> {
                if (!cheie.startsWith(piata + " |")) {
                    return;
                }
                sb.append(cheie.substring(piata.length() + 3)).append(":");
                a.calibrare.forEach((bin, v) -> sb.append(String.format("  %2.0f%%→%2.0f%% (n=%d)",
                        100 * v[0] / v[2], 100 * v[1] / v[2], (int) v[2])));
                sb.append('\n');
            });
        }
        StringBuilder diag = new StringBuilder("DIAGNOSTIC goluri pe meci (asteptat vs real):\n");
        diagnostice.forEach((nume, d) -> diag.append(String.format("  %-16s %.3f vs %.3f%n", nume, d[0] / d[2], d[1] / d[2])));
        sb.insert(0, sumar(baza) + diag + "\n");
        Path out = Path.of(System.getenv().getOrDefault("GOLSTAT_BACKTEST_OUT", "build/backtest-goluri.txt"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }
}
