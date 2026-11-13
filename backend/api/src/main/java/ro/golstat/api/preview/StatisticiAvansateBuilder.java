package ro.golstat.api.preview;

import ro.golstat.api.preview.StatisticiAvansateDto.EgaluriDto;
import ro.golstat.api.preview.StatisticiAvansateDto.FrecventaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.GgDto;
import ro.golstat.api.preview.StatisticiAvansateDto.CornereReprizeDto;
import ro.golstat.api.preview.StatisticiAvansateDto.LinieDto;
import ro.golstat.api.preview.StatisticiAvansateDto.LinieEchipaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.MediiEchipaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataEchipaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PieteEchipeDto;
import ro.golstat.api.preview.StatisticiAvansateDto.ReprizeDto;
import ro.golstat.api.preview.StatisticiAvansateDto.RezultatDto;
import ro.golstat.api.stats.CountLeagueAverages;
import ro.golstat.api.stats.IstoricCounturi;
import ro.golstat.stats.cards.CardMarket;
import ro.golstat.stats.counts.EventCountForm;
import ro.golstat.stats.counts.EventLineBlend;
import ro.golstat.stats.counts.TeamCountModel;
import ro.golstat.stats.counts.TeamCountModel.LambdaMeci;
import ro.golstat.stats.form.MatchWindow;
import ro.golstat.stats.form.WindowCounts;
import ro.golstat.stats.goals.HalfMarkets;
import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.match.MatchPrediction;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Compune {@link StatisticiAvansateDto} din ferestrele deja selectate ale celor doua echipe.
 * Calcul pur. Goluri / GG / egal vin din predictia meciului; cornerele, faulturile si cartonasele
 * din {@code TeamCountModel} (λ gazde + λ oaspeti pe ultimele 7 meciuri acasa / in deplasare, ancorat pe
 * media ligii, factor de arbitru la cartonase); suturile din blend-ul pe ferestrele de locatie. Frecventele
 * empirice ({@link WindowCounts}) raman pe ferestrele de locatie / generale, ca materie pentru legenda.
 */
public final class StatisticiAvansateBuilder {

    public static final double[] LINII_GOLURI = {1.5, 2.5, 3.5};
    public static final double[] LINII_CORNERE = {7.5, 8.5, 9.5, 10.5};
    public static final double[] LINII_FAULTURI = EventLineBlend.STANDARD_FOUL_LINES;
    public static final double[] LINII_CARTONASE = CardMarket.STANDARD_LINES;
    static final double[] LINII_SUTURI = {22.5, 24.5, 26.5};
    static final double[] LINII_SUTURI_POARTA = {7.5, 8.5, 9.5};
    public static final double[] LINII_ECHIPA_CORNERE = {2.5, 3.5, 4.5, 5.5};
    public static final double[] LINII_ECHIPA_CARTONASE = {0.5, 1.5, 2.5};
    public static final double[] LINII_ECHIPA_GOLURI = {0.5, 1.5, 2.5};
    public static final double[] LINII_REPRIZA_CORNERE = {3.5, 4.5, 5.5};
    public static final double[] LINII_REPRIZA_ECHIPA_CORNERE = {1.5, 2.5};
    /** Dispersiile Negative Binomial: faulturi 30, cartonase 8 — valorile cu care a rulat backtest-ul. */
    static final double DISPERSIE_FAULTURI = 30;
    static final double DISPERSIE_CARTONASE = 8;
    /** Suturile totale variaza cu ritmul meciului ca faulturile — aceeasi supra-dispersie. */
    static final double DISPERSIE_SUTURI = 30;

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
            List<EventCountSample> suturiPePoartaGeneral,
            List<EventCountSample> cornereRepriza1Locatie,
            List<EventCountSample> cornereRepriza1General,
            List<EventCountSample> cornereRepriza2Locatie,
            List<EventCountSample> cornereRepriza2General,
            Forma forma
    ) {
    }

    /** Ferestrele pe care lucreaza modelul atac × aparare: ultimele meciuri pe locatia echipei. */
    public record Forma(
            List<EventCountSample> cornere,
            List<EventCountSample> faulturi,
            List<EventCountSample> cartonase,
            List<EventCountSample> cornereRepriza1,
            List<EventCountSample> cornereRepriza2
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
                MatchWindow.lastN(counturi.suturiPePoarta(), fereastra),
                MatchWindow.lastN(counturi.cornereRepriza1(), fereastra, locatie),
                MatchWindow.lastN(counturi.cornereRepriza1(), fereastra),
                MatchWindow.lastN(counturi.cornereRepriza2(), fereastra, locatie),
                MatchWindow.lastN(counturi.cornereRepriza2(), fereastra),
                new Forma(
                        MatchWindow.lastN(counturi.cornere(), fereastra, locatie),
                        MatchWindow.lastN(counturi.faulturi(), fereastra, locatie),
                        MatchWindow.lastN(counturi.cartonase(), fereastra, locatie),
                        MatchWindow.lastN(counturi.cornereRepriza1(), fereastra, locatie),
                        MatchWindow.lastN(counturi.cornereRepriza2(), fereastra, locatie)));
    }

    /**
     * {@code modelGoluri} = predictia meciului ({@code MatchGoalModel}): liniile de goluri, GG si egalul
     * final vin de aici, ca pagina sa arate aceeasi cifra in predictie si in analiza pe piete.
     * {@code rezultat} = totalurile reale ale meciului (doar la meciuri terminate), pentru hit/miss.
     */
    public static StatisticiAvansateDto build(FerestreEchipa gazde, FerestreEchipa oaspeti,
                                              CountLeagueAverages mediiCounturi, double factorArbitru,
                                              MatchPrediction modelGoluri, RezultatDto rezultat) {
        LambdaMeci cornere = lambdas(mediiCounturi.cornere(), gazde.forma().cornere(), oaspeti.forma().cornere());
        LambdaMeci faulturi = lambdas(mediiCounturi.faulturi(), gazde.forma().faulturi(), oaspeti.forma().faulturi());
        LambdaMeci cartonase = lambdas(mediiCounturi.cartonase(),
                gazde.forma().cartonase(), oaspeti.forma().cartonase());
        return new StatisticiAvansateDto(
                goluri(gazde, oaspeti, modelGoluri),
                gg(gazde, oaspeti, modelGoluri),
                counturi(gazde.cornereLocatie(), gazde.cornereGeneral(),
                        oaspeti.cornereLocatie(), oaspeti.cornereGeneral(),
                        TeamCountModel.poisson(cornere.total(),
                                esantion(gazde.forma().cornere(), oaspeti.forma().cornere()), LINII_CORNERE)),
                counturi(gazde.faulturiLocatie(), gazde.faulturiGeneral(),
                        oaspeti.faulturiLocatie(), oaspeti.faulturiGeneral(),
                        TeamCountModel.overDispersed(faulturi.total(), DISPERSIE_FAULTURI,
                                esantion(gazde.forma().faulturi(), oaspeti.forma().faulturi()), LINII_FAULTURI)),
                counturi(gazde.cartonaseLocatie(), gazde.cartonaseGeneral(),
                        oaspeti.cartonaseLocatie(), oaspeti.cartonaseGeneral(),
                        TeamCountModel.overDispersed(cartonase.total() * factorArbitru, DISPERSIE_CARTONASE,
                                esantion(gazde.forma().cartonase(), oaspeti.forma().cartonase()), LINII_CARTONASE)),
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
                egaluri(gazde.goluriLocatie(), oaspeti.goluriLocatie(), modelGoluri.sansaEgal()),
                reprize(gazde.goluriLocatie(), oaspeti.goluriLocatie()),
                rezultat,
                new PieteEchipeDto(
                        piataEchipa(cornere.gazde(), cornere.oaspeti(), LINII_ECHIPA_CORNERE, Poisson::probabilityOver,
                                gazde.cornereLocatie(), gazde.cornereGeneral(),
                                oaspeti.cornereLocatie(), oaspeti.cornereGeneral(),
                                EventCountSample::countFor, EventCountSample::countAgainst),
                        piataEchipa(cartonase.gazde() * factorArbitru, cartonase.oaspeti() * factorArbitru,
                                LINII_ECHIPA_CARTONASE,
                                (l, linie) -> NegativeBinomial.probabilityOver(l, DISPERSIE_CARTONASE, linie),
                                gazde.cartonaseLocatie(), gazde.cartonaseGeneral(),
                                oaspeti.cartonaseLocatie(), oaspeti.cartonaseGeneral(),
                                EventCountSample::countFor, EventCountSample::countAgainst),
                        piataEchipa(modelGoluri.lambdaGazde(), modelGoluri.lambdaOaspeti(), LINII_ECHIPA_GOLURI,
                                Poisson::probabilityOver,
                                gazde.goluriLocatie(), gazde.goluriGeneral(),
                                oaspeti.goluriLocatie(), oaspeti.goluriGeneral(),
                                MatchSample::goalsFor, MatchSample::goalsAgainst)),
                cornereReprize(gazde, oaspeti, mediiCounturi));
    }

    /** λ pe repriza: aceeasi formula atac × aparare, pe cornerele din repriza respectiva. */
    private static CornereReprizeDto cornereReprize(FerestreEchipa gazde, FerestreEchipa oaspeti,
                                                    CountLeagueAverages medii) {
        LambdaMeci r1 = lambdas(medii.cornereRepriza1(),
                gazde.forma().cornereRepriza1(), oaspeti.forma().cornereRepriza1());
        LambdaMeci r2 = lambdas(medii.cornereRepriza2(),
                gazde.forma().cornereRepriza2(), oaspeti.forma().cornereRepriza2());
        return new CornereReprizeDto(
                counturi(gazde.cornereRepriza1Locatie(), gazde.cornereRepriza1General(),
                        oaspeti.cornereRepriza1Locatie(), oaspeti.cornereRepriza1General(),
                        TeamCountModel.poisson(r1.total(), esantion(gazde.forma().cornereRepriza1(),
                                oaspeti.forma().cornereRepriza1()), LINII_REPRIZA_CORNERE)),
                counturi(gazde.cornereRepriza2Locatie(), gazde.cornereRepriza2General(),
                        oaspeti.cornereRepriza2Locatie(), oaspeti.cornereRepriza2General(),
                        TeamCountModel.poisson(r2.total(), esantion(gazde.forma().cornereRepriza2(),
                                oaspeti.forma().cornereRepriza2()), LINII_REPRIZA_CORNERE)),
                piataEchipa(r1.gazde(), r1.oaspeti(), LINII_REPRIZA_ECHIPA_CORNERE, Poisson::probabilityOver,
                        gazde.cornereRepriza1Locatie(), gazde.cornereRepriza1General(),
                        oaspeti.cornereRepriza1Locatie(), oaspeti.cornereRepriza1General(),
                        EventCountSample::countFor, EventCountSample::countAgainst),
                piataEchipa(r2.gazde(), r2.oaspeti(), LINII_REPRIZA_ECHIPA_CORNERE, Poisson::probabilityOver,
                        gazde.cornereRepriza2Locatie(), gazde.cornereRepriza2General(),
                        oaspeti.cornereRepriza2Locatie(), oaspeti.cornereRepriza2General(),
                        EventCountSample::countFor, EventCountSample::countAgainst));
    }

    /** P(X > linie) pentru o asteptare data. */
    @FunctionalInterface
    private interface Distributie {
        double peste(double lambda, double linie);
    }

    /**
     * Piata per echipa: probabilitatea din λ-ul echipei; frecventele = cate a facut echipa (locatie /
     * general) si cate a primit adversarul pe locatia lui.
     */
    private static <T> PiataEchipaDto piataEchipa(double lambdaGazde, double lambdaOaspeti, double[] linii,
                                                  Distributie distributie,
                                                  List<T> gazdeLoc, List<T> gazdeGen,
                                                  List<T> oaspetiLoc, List<T> oaspetiGen,
                                                  ToIntFunction<T> facute, ToIntFunction<T> primite) {
        List<LinieEchipaDto> gazde = Arrays.stream(linii)
                .mapToObj(linie -> new LinieEchipaDto(linie, distributie.peste(lambdaGazde, linie),
                        peste(gazdeLoc, facute, linie), peste(gazdeGen, facute, linie),
                        peste(oaspetiLoc, primite, linie)))
                .toList();
        List<LinieEchipaDto> oaspeti = Arrays.stream(linii)
                .mapToObj(linie -> new LinieEchipaDto(linie, distributie.peste(lambdaOaspeti, linie),
                        peste(oaspetiLoc, facute, linie), peste(oaspetiGen, facute, linie),
                        peste(gazdeLoc, primite, linie)))
                .toList();
        return new PiataEchipaDto(rotunjit(lambdaGazde), rotunjit(lambdaOaspeti), gazde, oaspeti);
    }

    private static <T> FrecventaDto peste(List<T> fereastra, ToIntFunction<T> valoare, double linie) {
        int reusite = (int) fereastra.stream().filter(m -> valoare.applyAsInt(m) > linie).count();
        return new FrecventaDto(reusite, fereastra.size());
    }

    private static double rotunjit(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    private static LambdaMeci lambdas(CountLeagueAverages.PeLocatie medii,
                                      List<EventCountSample> gazde, List<EventCountSample> oaspeti) {
        return TeamCountModel.lambdas(medii.gazde(), medii.oaspeti(),
                EventCountForm.of(gazde), EventCountForm.of(oaspeti));
    }

    private static int esantion(List<?> gazde, List<?> oaspeti) {
        return gazde.size() + oaspeti.size();
    }

    private static PiataDto goluri(FerestreEchipa gazde, FerestreEchipa oaspeti, MatchPrediction model) {
        List<LinieDto> linii = Arrays.stream(LINII_GOLURI)
                .mapToObj(linie -> new LinieDto(linie, model.linie(linie).overRate(),
                        frecventaGol(gazde.goluriLocatie(), linie),
                        frecventaGol(gazde.goluriGeneral(), linie),
                        frecventaGol(oaspeti.goluriLocatie(), linie),
                        frecventaGol(oaspeti.goluriGeneral(), linie)))
                .toList();
        return new PiataDto(linii,
                mediiGol(gazde.goluriLocatie(), gazde.goluriGeneral()),
                mediiGol(oaspeti.goluriLocatie(), oaspeti.goluriGeneral()));
    }

    private static GgDto gg(FerestreEchipa gazde, FerestreEchipa oaspeti, MatchPrediction model) {
        return new GgDto(model.btts(),
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
                                      double egalFinalModel) {
        if (gazdeLoc.isEmpty() && oaspetiLoc.isEmpty()) {
            return null;
        }
        HalfMarkets.HalfMarketStats reprize = HalfMarkets.of(gazdeLoc, oaspetiLoc);
        return new EgaluriDto(reprize.htDrawRate(), egalFinalModel,
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
