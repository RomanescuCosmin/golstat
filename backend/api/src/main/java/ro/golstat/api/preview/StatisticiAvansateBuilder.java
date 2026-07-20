package ro.golstat.api.preview;

import ro.golstat.api.preview.StatisticiAvansateDto.EgaluriDto;
import ro.golstat.api.preview.StatisticiAvansateDto.FrecventaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.GgDto;
import ro.golstat.api.preview.StatisticiAvansateDto.LinieDto;
import ro.golstat.api.preview.StatisticiAvansateDto.MediiEchipaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataDto;
import ro.golstat.api.preview.StatisticiAvansateDto.ReprizeDto;
import ro.golstat.api.preview.StatisticiAvansateDto.RezultatDto;
import ro.golstat.api.stats.CountLeagueAverages;
import ro.golstat.api.stats.IstoricCounturi;
import ro.golstat.stats.cards.CardMarket;
import ro.golstat.stats.counts.EventLineBlend;
import ro.golstat.stats.form.MatchWindow;
import ro.golstat.stats.form.WindowCounts;
import ro.golstat.stats.goals.GoalLineBlend;
import ro.golstat.stats.goals.GoalLineStats;
import ro.golstat.stats.goals.HalfMarkets;
import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Compune {@link StatisticiAvansateDto} din ferestrele deja selectate ale celor doua echipe.
 * Calcul pur — probabilitatile modelate vin din {@code stats-engine} (blend empiric ↔ distributie,
 * shrinkage spre media ligii, factor de arbitru la cartonase), frecventele empirice din
 * {@link WindowCounts}. Ferestrele "locatie" ale ambelor echipe, concatenate, dau fereastra
 * modelului pe meci.
 */
public final class StatisticiAvansateBuilder {

    public static final double[] LINII_GOLURI = {1.5, 2.5, 3.5};
    public static final double[] LINII_CORNERE = {7.5, 8.5, 9.5, 10.5};
    public static final double[] LINII_FAULTURI = EventLineBlend.STANDARD_FOUL_LINES;
    public static final double[] LINII_CARTONASE = CardMarket.STANDARD_LINES;
    static final double[] LINII_SUTURI = {22.5, 24.5, 26.5};
    static final double[] LINII_SUTURI_POARTA = {7.5, 8.5, 9.5};
    /** Dispersiile Negative Binomial calibrate in stats-engine (vezi EventLineBlendTest / CardMarketTest). */
    static final double DISPERSIE_FAULTURI = 30;
    static final double DISPERSIE_CARTONASE = 8;
    /** Suturile totale variaza cu ritmul meciului ca faulturile — aceeasi supra-dispersie. */
    static final double DISPERSIE_SUTURI = 30;
    /** Constanta de incredere la blend-ul de egal final: {@code w = n/(n+K)}, ca in motor. */
    static final int K = 3;

    /** Ferestrele unei echipe: goluri + counturi, fiecare pe locatie si general. */
    public record FerestreEchipa(
            List<MatchSample> goluriLocatie,
            List<MatchSample> goluriGeneral,
            List<EventCountSample> cornereLocatie,
            List<EventCountSample> cornereGeneral,
            List<EventCountSample> faulturiLocatie,
            List<EventCountSample> faulturiGeneral,
            List<EventCountSample> cartonaseLocatie,
            List<EventCountSample> cartonaseGeneral,
            List<EventCountSample> suturiLocatie,
            List<EventCountSample> suturiGeneral,
            List<EventCountSample> suturiPePoartaLocatie,
            List<EventCountSample> suturiPePoartaGeneral
    ) {
    }

    private StatisticiAvansateBuilder() {
    }

    /**
     * Ferestrele unei echipe din istoricul ei deja incarcat: ultimele {@code fereastra} meciuri pe
     * {@code locatie} si ultimele {@code fereastra} generale, pentru fiecare piata.
     */
    public static FerestreEchipa ferestre(List<MatchSample> istoric, IstoricCounturi counturi,
                                          MatchLocation locatie, int fereastra) {
        return new FerestreEchipa(
                MatchWindow.lastN(istoric, fereastra, locatie),
                MatchWindow.lastN(istoric, fereastra),
                MatchWindow.lastN(counturi.cornere(), fereastra, locatie),
                MatchWindow.lastN(counturi.cornere(), fereastra),
                MatchWindow.lastN(counturi.faulturi(), fereastra, locatie),
                MatchWindow.lastN(counturi.faulturi(), fereastra),
                MatchWindow.lastN(counturi.cartonase(), fereastra, locatie),
                MatchWindow.lastN(counturi.cartonase(), fereastra),
                MatchWindow.lastN(counturi.suturi(), fereastra, locatie),
                MatchWindow.lastN(counturi.suturi(), fereastra),
                MatchWindow.lastN(counturi.suturiPePoarta(), fereastra, locatie),
                MatchWindow.lastN(counturi.suturiPePoarta(), fereastra));
    }

    /**
     * {@code egalFinalModel} = P(egal) 0..1 din modelul de goluri al meciului; null daca lipseste.
     * {@code rezultat} = totalurile reale ale meciului (doar la meciuri terminate), pentru hit/miss.
     */
    public static StatisticiAvansateDto build(FerestreEchipa gazde, FerestreEchipa oaspeti,
                                              CountLeagueAverages mediiCounturi, double mediaGoluriLiga,
                                              double factorArbitru, Double egalFinalModel, RezultatDto rezultat) {
        GoalLineStats modelGoluri = GoalLineBlend.of(
                concat(gazde.goluriLocatie(), oaspeti.goluriLocatie()), mediaGoluriLiga, LINII_GOLURI);
        return new StatisticiAvansateDto(
                goluri(gazde, oaspeti, modelGoluri),
                gg(gazde, oaspeti, modelGoluri),
                counturi(gazde.cornereLocatie(), gazde.cornereGeneral(),
                        oaspeti.cornereLocatie(), oaspeti.cornereGeneral(),
                        EventLineBlend.poisson(
                                concat(gazde.cornereLocatie(), oaspeti.cornereLocatie()),
                                mediiCounturi.cornerePeMeci(), LINII_CORNERE)),
                counturi(gazde.faulturiLocatie(), gazde.faulturiGeneral(),
                        oaspeti.faulturiLocatie(), oaspeti.faulturiGeneral(),
                        EventLineBlend.overDispersed(
                                concat(gazde.faulturiLocatie(), oaspeti.faulturiLocatie()),
                                mediiCounturi.faulturiPeMeci(), DISPERSIE_FAULTURI, LINII_FAULTURI)),
                counturi(gazde.cartonaseLocatie(), gazde.cartonaseGeneral(),
                        oaspeti.cartonaseLocatie(), oaspeti.cartonaseGeneral(),
                        CardMarket.of(concat(gazde.cartonaseLocatie(), oaspeti.cartonaseLocatie()),
                                mediiCounturi.cartonasePeMeci(), factorArbitru,
                                DISPERSIE_CARTONASE, LINII_CARTONASE)),
                counturi(gazde.suturiLocatie(), gazde.suturiGeneral(),
                        oaspeti.suturiLocatie(), oaspeti.suturiGeneral(),
                        EventLineBlend.overDispersed(
                                concat(gazde.suturiLocatie(), oaspeti.suturiLocatie()),
                                mediiCounturi.suturiPeMeci(), DISPERSIE_SUTURI, LINII_SUTURI)),
                counturi(gazde.suturiPePoartaLocatie(), gazde.suturiPePoartaGeneral(),
                        oaspeti.suturiPePoartaLocatie(), oaspeti.suturiPePoartaGeneral(),
                        EventLineBlend.poisson(
                                concat(gazde.suturiPePoartaLocatie(), oaspeti.suturiPePoartaLocatie()),
                                mediiCounturi.suturiPePoartaPeMeci(), LINII_SUTURI_POARTA)),
                egaluri(gazde.goluriLocatie(), oaspeti.goluriLocatie(), egalFinalModel),
                reprize(gazde.goluriLocatie(), oaspeti.goluriLocatie()),
                rezultat);
    }

    private static PiataDto goluri(FerestreEchipa gazde, FerestreEchipa oaspeti, GoalLineStats model) {
        List<LinieDto> linii = Arrays.stream(LINII_GOLURI)
                .mapToObj(linie -> new LinieDto(linie, model.line(linie).overRate(),
                        frecventaGol(gazde.goluriLocatie(), linie),
                        frecventaGol(gazde.goluriGeneral(), linie),
                        frecventaGol(oaspeti.goluriLocatie(), linie),
                        frecventaGol(oaspeti.goluriGeneral(), linie)))
                .toList();
        return new PiataDto(linii,
                mediiGol(gazde.goluriLocatie(), gazde.goluriGeneral()),
                mediiGol(oaspeti.goluriLocatie(), oaspeti.goluriGeneral()));
    }

    private static GgDto gg(FerestreEchipa gazde, FerestreEchipa oaspeti, GoalLineStats model) {
        return new GgDto(model.bttsRate(),
                new FrecventaDto(WindowCounts.scored(gazde.goluriLocatie()), gazde.goluriLocatie().size()),
                new FrecventaDto(WindowCounts.conceded(gazde.goluriLocatie()), gazde.goluriLocatie().size()),
                new FrecventaDto(WindowCounts.scored(oaspeti.goluriLocatie()), oaspeti.goluriLocatie().size()),
                new FrecventaDto(WindowCounts.conceded(oaspeti.goluriLocatie()), oaspeti.goluriLocatie().size()));
    }

    private static PiataDto counturi(List<EventCountSample> gazdeLoc, List<EventCountSample> gazdeGen,
                                     List<EventCountSample> oaspetiLoc, List<EventCountSample> oaspetiGen,
                                     EventLineStats model) {
        List<LinieDto> linii = model.lines().stream()
                .map(ou -> new LinieDto(ou.line(), ou.overRate(),
                        frecventaCount(gazdeLoc, ou.line()),
                        frecventaCount(gazdeGen, ou.line()),
                        frecventaCount(oaspetiLoc, ou.line()),
                        frecventaCount(oaspetiGen, ou.line())))
                .toList();
        return new PiataDto(linii, mediiCount(gazdeLoc, gazdeGen), mediiCount(oaspetiLoc, oaspetiGen));
    }

    private static EgaluriDto egaluri(List<MatchSample> gazdeLoc, List<MatchSample> oaspetiLoc,
                                      Double egalFinalModel) {
        int n = gazdeLoc.size() + oaspetiLoc.size();
        if (n == 0) {
            return null;
        }
        HalfMarkets.HalfMarketStats reprize = HalfMarkets.of(gazdeLoc, oaspetiLoc);
        int egaleFt = WindowCounts.drawsFullTime(gazdeLoc) + WindowCounts.drawsFullTime(oaspetiLoc);
        double empiric = (double) egaleFt / n;
        double w = (double) n / (n + K);
        double egalFinal = egalFinalModel != null ? w * empiric + (1 - w) * egalFinalModel : empiric;
        return new EgaluriDto(reprize.htDrawRate(), egalFinal,
                new FrecventaDto(WindowCounts.drawsHalfTime(gazdeLoc), gazdeLoc.size()),
                new FrecventaDto(WindowCounts.drawsHalfTime(oaspetiLoc), oaspetiLoc.size()),
                new FrecventaDto(WindowCounts.drawsFullTime(gazdeLoc), gazdeLoc.size()),
                new FrecventaDto(WindowCounts.drawsFullTime(oaspetiLoc), oaspetiLoc.size()));
    }

    private static ReprizeDto reprize(List<MatchSample> gazdeLoc, List<MatchSample> oaspetiLoc) {
        if (gazdeLoc.isEmpty() && oaspetiLoc.isEmpty()) {
            return null;
        }
        HalfMarkets.HalfMarketStats model = HalfMarkets.of(gazdeLoc, oaspetiLoc);
        return new ReprizeDto(model.goalInFirstHalfRate(), model.goalInSecondHalfRate(),
                new FrecventaDto(WindowCounts.goalInFirstHalf(gazdeLoc), gazdeLoc.size()),
                new FrecventaDto(WindowCounts.goalInFirstHalf(oaspetiLoc), oaspetiLoc.size()),
                new FrecventaDto(WindowCounts.goalInSecondHalf(gazdeLoc), gazdeLoc.size()),
                new FrecventaDto(WindowCounts.goalInSecondHalf(oaspetiLoc), oaspetiLoc.size()));
    }

    private static FrecventaDto frecventaGol(List<MatchSample> fereastra, double linie) {
        return new FrecventaDto(WindowCounts.overTotalGoals(fereastra, linie), fereastra.size());
    }

    private static FrecventaDto frecventaCount(List<EventCountSample> fereastra, double linie) {
        return new FrecventaDto(WindowCounts.overTotalEvents(fereastra, linie), fereastra.size());
    }

    private static MediiEchipaDto mediiGol(List<MatchSample> locatie, List<MatchSample> general) {
        return new MediiEchipaDto(
                medie(locatie, MatchSample::goalsFor),
                medie(locatie, m -> m.goalsFor() + m.goalsAgainst()),
                medie(general, MatchSample::goalsFor),
                medie(general, m -> m.goalsFor() + m.goalsAgainst()));
    }

    private static MediiEchipaDto mediiCount(List<EventCountSample> locatie, List<EventCountSample> general) {
        return new MediiEchipaDto(
                medie(locatie, EventCountSample::countFor),
                medie(locatie, s -> s.countFor() + s.countAgainst()),
                medie(general, EventCountSample::countFor),
                medie(general, s -> s.countFor() + s.countAgainst()));
    }

    private static <T> Double medie(List<T> fereastra, ToIntFunction<T> valoare) {
        if (fereastra.isEmpty()) {
            return null;
        }
        double medie = fereastra.stream().mapToInt(valoare).average().orElse(0.0);
        return Math.round(medie * 100.0) / 100.0;
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }
}
