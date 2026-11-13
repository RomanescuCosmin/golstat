package ro.golstat.api.backtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ro.golstat.api.prediction.PredictionService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.stats.cards.CardMarket;
import ro.golstat.stats.counts.EventCountForm;
import ro.golstat.stats.counts.EventCountStats;
import ro.golstat.stats.counts.EventLineBlend;
import ro.golstat.stats.counts.TeamCountModel;
import ro.golstat.stats.goals.GoalLineBlend;
import ro.golstat.stats.goals.GoalLineStats;
import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.match.MatchPrediction;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.ScoreGrid;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.model.MatchSample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Backtest al pietelor pe baza reala: pentru fiecare meci terminat calculeaza procentele DOAR din
 * meciurile dinaintea lui si le compara cu ce s-a intamplat. Raporteaza scorul Brier (mai mic = mai
 * bine), „skill"-ul fata de rata de baza a ligii (>0 = modelul chiar ajuta) si calibrarea
 * (media prezisa vs frecventa reala).
 *
 * <p>Nu e test de regresie: ruleaza doar cu {@code GOLSTAT_BACKTEST=1}, citeste din MySQL-ul local
 * si scrie raportul in {@code build/backtest.txt}.
 */
@EnabledIfEnvironmentVariable(named = "GOLSTAT_BACKTEST", matches = "1")
class BacktestPieteTest {

    private static final int FEREASTRA = 7;
    private static final int MIN_FEREASTRA = 3;
    /** Sub atatea meciuri jucate in liga, media ligii e prea zgomotoasa ca ancora. */
    private static final int MIN_MECIURI_LIGA = 20;

    /** Un meci din perspectiva unei echipe; campurile de count sunt null cand lipsesc. */
    record M(LocalDateTime kickoff, boolean home, int gf, int ga, int htf, int hta,
             Integer corF, Integer corA, Integer foulF, Integer foulA, Integer cardF, Integer cardA,
             Integer cor1F, Integer cor1A, Integer cor2F, Integer cor2A) {
    }

    record Meci(long id, long liga, LocalDateTime kickoff, long home, long away, M dinParteaGazdei) {
    }

    /** Extrage (facute, primite) pentru o piata; null = fara date la acel meci. */
    record Piata(String nume, Function<M, Integer> facute, Function<M, Integer> primite,
                 double[] liniiTotal, double[] liniiGazde, double[] liniiOaspeti, Double dispersie) {
    }

    static final class Acc {
        double brier;
        double pred;
        double actual;
        int n;

        void add(double p, boolean hit) {
            double y = hit ? 1 : 0;
            brier += (p - y) * (p - y);
            pred += p;
            actual += y;
            n++;
        }
    }

    private final Map<String, Acc> rezultate = new LinkedHashMap<>();

    @Test
    void backtest() throws Exception {
        List<Meci> meciuri = incarca();
        Map<Long, List<M>> istoric = new HashMap<>();
        // medii de liga CUMULATIVE: la fiecare meci doar ce s-a jucat inaintea lui (fara informatie din viitor)
        Map<Long, double[]> sume = new HashMap<>();
        Map<Long, int[]> numere = new HashMap<>();

        List<Piata> piete = List.of(
                new Piata("cornere", M::corF, M::corA, new double[]{8.5, 9.5, 10.5},
                        new double[]{3.5, 4.5, 5.5}, new double[]{2.5, 3.5, 4.5}, null),
                new Piata("cartonase", M::cardF, M::cardA, new double[]{3.5, 4.5, 5.5},
                        new double[]{1.5, 2.5}, new double[]{1.5, 2.5}, 8.0),
                new Piata("faulturi", M::foulF, M::foulA, new double[]{21.5, 24.5, 27.5},
                        new double[]{10.5, 12.5}, new double[]{10.5, 12.5}, 30.0),
                new Piata("cornere-R1", M::cor1F, M::cor1A, new double[]{3.5, 4.5, 5.5},
                        new double[]{1.5, 2.5}, new double[]{1.5, 2.5}, null),
                new Piata("cornere-R2", M::cor2F, M::cor2A, new double[]{3.5, 4.5, 5.5},
                        new double[]{1.5, 2.5}, new double[]{1.5, 2.5}, null));

        for (Meci meci : meciuri) {
            List<M> gazde = istoric.getOrDefault(meci.home(), List.of());
            List<M> oaspeti = istoric.getOrDefault(meci.away(), List.of());
            M real = meci.dinParteaGazdei();
            double[] s = sume.computeIfAbsent(meci.liga(), k -> new double[12]);
            int[] n = numere.computeIfAbsent(meci.liga(), k -> new int[12]);

            if (n[10] >= MIN_MECIURI_LIGA) {
                for (int i = 0; i < piete.size(); i++) {
                    if (n[2 * i] >= MIN_MECIURI_LIGA) {
                        evalueazaCount(piete.get(i), gazde, oaspeti, real, s[2 * i] / n[2 * i], s[2 * i + 1] / n[2 * i + 1]);
                    }
                }
                evalueazaGoluri(gazde, oaspeti, real, s[10] / n[10], s[11] / n[11]);
            }

            Integer[] v = {real.corF(), real.corA(), real.cardF(), real.cardA(), real.foulF(), real.foulA(),
                    real.cor1F(), real.cor1A(), real.cor2F(), real.cor2A(), real.gf(), real.ga()};
            for (int i = 0; i < v.length; i++) {
                if (v[i] != null) {
                    s[i] += v[i];
                    n[i]++;
                }
            }
            istoric.computeIfAbsent(meci.home(), k -> new ArrayList<>()).add(real);
            istoric.computeIfAbsent(meci.away(), k -> new ArrayList<>()).add(intors(real));
        }
        scrieRaport(meciuri.size());
    }

    private void evalueazaCount(Piata p, List<M> gazde, List<M> oaspeti, M real,
                                double mediaGazde, double mediaOaspeti) {
        Integer realF = p.facute().apply(real);
        Integer realA = p.primite().apply(real);
        if (realF == null || realA == null) {
            return;
        }
        List<EventCountSample> gLoc = fereastra(gazde, p, true, FEREASTRA);
        List<EventCountSample> oLoc = fereastra(oaspeti, p, false, FEREASTRA);
        if (gLoc.size() < MIN_FEREASTRA || oLoc.size() < MIN_FEREASTRA) {
            return;
        }
        List<EventCountSample> concat = Stream.concat(gLoc.stream(), oLoc.stream()).toList();
        double mediaTotal = mediaGazde + mediaOaspeti;
        int total = realF + realA;

        EventLineStats actual;
        if ("cartonase".equals(p.nume())) {
            actual = CardMarket.of(concat, mediaTotal, 1.0, p.dispersie(), p.liniiTotal());
        } else if (p.dispersie() != null) {
            actual = EventLineBlend.overDispersed(concat, mediaTotal, p.dispersie(), p.liniiTotal());
        } else {
            actual = EventLineBlend.poisson(concat, mediaTotal, p.liniiTotal());
        }

        for (double linie : p.liniiTotal()) {
            String cheie = p.nume() + " total " + linie;
            boolean hit = total > linie;
            acc(cheie, "0 rata-ligii").add(over(mediaTotal, p.dispersie(), linie), hit);
            acc(cheie, "1 ACTUAL").add(actual.line(linie).overRate(), hit);
        }

        for (double k : new double[]{15, 20, 30}) {
            for (boolean toate : new boolean[]{false, true}) {
                double[] l = lambdas(gazde, oaspeti, p, mediaGazde, mediaOaspeti, k, toate);
                String v = "2 NOU k=" + (int) k + (toate ? " toate-loc" : " pe-loc");
                for (double linie : p.liniiTotal()) {
                    acc(p.nume() + " total " + linie, v).add(over(l[0] + l[1], p.dispersie(), linie), total > linie);
                }
                for (double linie : p.liniiGazde()) {
                    acc(p.nume() + " gazde " + linie, v).add(over(l[0], p.dispersie(), linie), realF > linie);
                }
                for (double linie : p.liniiOaspeti()) {
                    acc(p.nume() + " oaspeti " + linie, v).add(over(l[1], p.dispersie(), linie), realA > linie);
                }
            }
        }
        for (double linie : p.liniiGazde()) {
            acc(p.nume() + " gazde " + linie, "0 rata-ligii").add(over(mediaGazde, p.dispersie(), linie), realF > linie);
        }
        for (double linie : p.liniiOaspeti()) {
            acc(p.nume() + " oaspeti " + linie, "0 rata-ligii").add(over(mediaOaspeti, p.dispersie(), linie), realA > linie);
        }
    }

    /**
     * λ gazde / oaspeti cu {@link TeamCountModel}. {@code toateLocatiile}: forta echipei din TOATE meciurile ei
     * (esantion dublu), raportata la media ligii pe echipa, apoi aplicata pe media locatiei.
     */
    private static double[] lambdas(List<M> gazde, List<M> oaspeti, Piata p, double mediaGazde, double mediaOaspeti,
                                    double k, boolean toateLocatiile) {
        if (!toateLocatiile) {
            EventCountStats g = EventCountForm.of(fereastra(gazde, p, true, 99));
            EventCountStats o = EventCountForm.of(fereastra(oaspeti, p, false, 99));
            return new double[]{TeamCountModel.lambda(mediaGazde, g, o, k), TeamCountModel.lambda(mediaOaspeti, o, g, k)};
        }
        double medie = (mediaGazde + mediaOaspeti) / 2;
        EventCountStats g = EventCountForm.of(toate(gazde, p));
        EventCountStats o = EventCountForm.of(toate(oaspeti, p));
        double fortaGazde = TeamCountModel.lambda(medie, g, o, k) / medie;
        double fortaOaspeti = TeamCountModel.lambda(medie, o, g, k) / medie;
        return new double[]{mediaGazde * fortaGazde, mediaOaspeti * fortaOaspeti};
    }

    private static List<EventCountSample> toate(List<M> istoric, Piata p) {
        return istoric.stream()
                .filter(m -> p.facute().apply(m) != null && p.primite().apply(m) != null)
                .map(m -> new EventCountSample(m.kickoff().toLocalDate(), m.home(),
                        p.facute().apply(m), p.primite().apply(m), null))
                .toList();
    }

    private void evalueazaGoluri(List<M> gazde, List<M> oaspeti, M real, double mediaGazde, double mediaOaspeti) {
        List<MatchSample> g = gazde.stream().map(BacktestPieteTest::sample).toList();
        List<MatchSample> o = oaspeti.stream().map(BacktestPieteTest::sample).toList();
        List<MatchSample> gLoc = ultimele(g.stream().filter(MatchSample::home).toList(), FEREASTRA);
        List<MatchSample> oLoc = ultimele(o.stream().filter(s -> !s.home()).toList(), FEREASTRA);
        if (gLoc.size() < MIN_FEREASTRA || oLoc.size() < MIN_FEREASTRA) {
            return;
        }
        double[] linii = {1.5, 2.5, 3.5};
        GoalLineStats blend = GoalLineBlend.of(
                Stream.concat(gLoc.stream(), oLoc.stream()).toList(), mediaGazde + mediaOaspeti, linii);
        MatchPrediction model = MatchGoalModel.predict(
                PredictionService.buildContext(g, o, new LeagueAverages(mediaGazde, mediaOaspeti)), linii);
        int total = real.gf() + real.ga();
        for (double linie : linii) {
            String cheie = "goluri total " + linie;
            boolean hit = total > linie;
            acc(cheie, "0 rata-ligii").add(Poisson.probabilityOver(mediaGazde + mediaOaspeti, linie), hit);
            acc(cheie, "1 ACTUAL statistici (blend)").add(blend.line(linie).overRate(), hit);
            acc(cheie, "1 ACTUAL predictie (model)").add(model.linie(linie).overRate(), hit);
        }
        boolean gg = real.gf() > 0 && real.ga() > 0;
        double ggLiga = (1 - Math.exp(-mediaGazde)) * (1 - Math.exp(-mediaOaspeti));
        acc("goluri GG", "0 rata-ligii").add(ggLiga, gg);
        acc("goluri GG", "1 ACTUAL statistici (blend)").add(blend.bttsRate(), gg);
        acc("goluri GG", "1 ACTUAL predictie (model)").add(model.btts(), gg);
        for (double linie : new double[]{0.5, 1.5}) {
            acc("goluri gazde " + linie, "0 rata-ligii").add(Poisson.probabilityOver(mediaGazde, linie), real.gf() > linie);
            acc("goluri gazde " + linie, "2 NOU (λ model)").add(
                    Poisson.probabilityOver(model.lambdaGazde(), linie), real.gf() > linie);
            acc("goluri oaspeti " + linie, "0 rata-ligii").add(Poisson.probabilityOver(mediaOaspeti, linie), real.ga() > linie);
            acc("goluri oaspeti " + linie, "2 NOU (λ model)").add(
                    Poisson.probabilityOver(model.lambdaOaspeti(), linie), real.ga() > linie);
        }
        Piata goluri = new Piata("goluri", M::gf, M::ga, null, null, null, null);
        for (double k : new double[]{15, 20, 30}) {
            for (boolean toateLoc : new boolean[]{false, true}) {
                double[] l = lambdas(gazde, oaspeti, goluri, mediaGazde, mediaOaspeti, k, toateLoc);
                ScoreGrid grid = ScoreGrid.dixonColes(l[0], l[1], MatchGoalModel.RHO, MatchGoalModel.MAX_GOALS);
                String v = "2 NOU k=" + (int) k + (toateLoc ? " toate-loc" : " pe-loc");
                for (double linie : linii) {
                    acc("goluri total " + linie, v).add(grid.probabilityOverTotal(linie), total > linie);
                }
                acc("goluri GG", v).add(grid.btts(), gg);
                acc("1X2 gazde", v).add(grid.homeWin(), real.gf() > real.ga());
                acc("1X2 egal", v).add(grid.draw(), real.gf() == real.ga());
                for (double linie : new double[]{0.5, 1.5}) {
                    acc("goluri gazde " + linie, v).add(Poisson.probabilityOver(l[0], linie), real.gf() > linie);
                    acc("goluri oaspeti " + linie, v).add(Poisson.probabilityOver(l[1], linie), real.ga() > linie);
                }
            }
        }
        acc("1X2 gazde", "0 rata-ligii").add(0.45, real.gf() > real.ga());
        acc("1X2 gazde", "1 ACTUAL predictie (model)").add(model.sansaGazde(), real.gf() > real.ga());
        acc("1X2 egal", "0 rata-ligii").add(0.25, real.gf() == real.ga());
        acc("1X2 egal", "1 ACTUAL predictie (model)").add(model.sansaEgal(), real.gf() == real.ga());
    }

    private static double over(double medie, Double dispersie, double linie) {
        return dispersie == null
                ? Poisson.probabilityOver(medie, linie)
                : NegativeBinomial.probabilityOver(medie, dispersie, linie);
    }

    private static List<EventCountSample> fereastra(List<M> istoric, Piata p, boolean acasa, int n) {
        List<EventCountSample> toate = istoric.stream()
                .filter(m -> m.home() == acasa)
                .filter(m -> p.facute().apply(m) != null && p.primite().apply(m) != null)
                .map(m -> new EventCountSample(m.kickoff().toLocalDate(), m.home(),
                        p.facute().apply(m), p.primite().apply(m), null))
                .toList();
        return toate.subList(Math.max(0, toate.size() - n), toate.size());
    }

    private static <T> List<T> ultimele(List<T> lista, int n) {
        return lista.subList(Math.max(0, lista.size() - n), lista.size());
    }

    private static MatchSample sample(M m) {
        return new MatchSample(m.kickoff().toLocalDate(), m.home(), m.gf(), m.ga(), m.htf(), m.hta(), null);
    }

    private static M intors(M m) {
        return new M(m.kickoff(), !m.home(), m.ga(), m.gf(), m.hta(), m.htf(),
                m.corA(), m.corF(), m.foulA(), m.foulF(), m.cardA(), m.cardF(),
                m.cor1A(), m.cor1F(), m.cor2A(), m.cor2F());
    }

    private Acc acc(String cheie, String varianta) {
        return rezultate.computeIfAbsent(cheie + " | " + varianta, k -> new Acc());
    }


    private static List<Meci> incarca() throws Exception {
        String url = System.getenv().getOrDefault("GOLSTAT_BACKTEST_URL",
                "jdbc:mysql://localhost:3306/golstat_bzz?serverTimezone=UTC");
        String sql = """
                select f.id, f.league_id, f.kickoff, f.home_team_id, f.away_team_id,
                       coalesce(f.score_ft_home, f.goals_home), coalesce(f.score_ft_away, f.goals_away),
                       coalesce(f.score_ht_home, 0), coalesce(f.score_ht_away, 0),
                       h.corner_kicks, a.corner_kicks, h.fouls, a.fouls,
                       h.yellow_cards + coalesce(h.red_cards, 0), a.yellow_cards + coalesce(a.red_cards, 0),
                       h1.corner_kicks, a1.corner_kicks, h2.corner_kicks, a2.corner_kicks
                from fixture f
                left join fixture_team_stats h on h.fixture_id = f.id and h.team_id = f.home_team_id
                left join fixture_team_stats a on a.fixture_id = f.id and a.team_id = f.away_team_id
                left join fixture_team_half_stats h1 on h1.fixture_id = f.id and h1.team_id = f.home_team_id and h1.half = 1
                left join fixture_team_half_stats a1 on a1.fixture_id = f.id and a1.team_id = f.away_team_id and a1.half = 1
                left join fixture_team_half_stats h2 on h2.fixture_id = f.id and h2.team_id = f.home_team_id and h2.half = 2
                left join fixture_team_half_stats a2 on a2.fixture_id = f.id and a2.team_id = f.away_team_id and a2.half = 2
                where f.status_short in ('FT', 'AET', 'PEN') and f.kickoff is not null
                """;
        List<Meci> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, "golstat", "golstat");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                LocalDateTime kickoff = rs.getTimestamp(3).toLocalDateTime();
                M m = new M(kickoff, true, rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9),
                        intOrNull(rs, 10), intOrNull(rs, 11), intOrNull(rs, 12), intOrNull(rs, 13),
                        intOrNull(rs, 14), intOrNull(rs, 15),
                        intOrNull(rs, 16), intOrNull(rs, 17), intOrNull(rs, 18), intOrNull(rs, 19));
                out.add(new Meci(rs.getLong(1), rs.getLong(2), kickoff, rs.getLong(4), rs.getLong(5), m));
            }
        }
        out.sort(Comparator.comparing(Meci::kickoff).thenComparing(Meci::id));
        return out;
    }

    private static Integer intOrNull(ResultSet rs, int col) throws Exception {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private void scrieRaport(int meciuri) throws Exception {
        StringBuilder sb = new StringBuilder("Backtest pe " + meciuri + " meciuri terminate\n");
        sb.append(String.format("%-26s %-30s %5s %7s %7s %8s %7s%n",
                "piata", "varianta", "n", "prezis", "real", "brier", "skill"));
        Map<String, Double> brierBaza = new HashMap<>();
        rezultate.forEach((cheie, a) -> {
            String[] p = cheie.split(" \\| ");
            if (p[1].startsWith("0 ")) {
                brierBaza.put(p[0], a.brier / a.n);
            }
        });
        rezultate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    String[] p = e.getKey().split(" \\| ");
                    Acc a = e.getValue();
                    double brier = a.brier / a.n;
                    double baza = brierBaza.getOrDefault(p[0], brier);
                    sb.append(String.format("%-26s %-30s %5d %6.1f%% %6.1f%% %8.4f %+6.1f%%%n",
                            p[0], p[1], a.n, 100 * a.pred / a.n, 100 * a.actual / a.n, brier,
                            baza > 0 ? 100 * (1 - brier / baza) : 0));
                });
        Path out = Path.of(System.getenv().getOrDefault("GOLSTAT_BACKTEST_OUT", "build/backtest.txt"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }
}
