package ro.golstat.api.preview;

import java.util.List;

/**
 * Analiza pe piete a meciului, pe ferestrele ultimelor 7 meciuri: gazdele ACASA si oaspetii in
 * DEPLASARE (ferestrele "locatie"), plus ferestrele generale ale fiecarei echipe pentru context.
 *
 * <p>{@code probabilitate} e intotdeauna rata MODELATA 0..1 pe totalul meciului (blend empiric ↔
 * distributie cu shrinkage spre media ligii; cartonasele includ factorul de arbitru).
 * {@link FrecventaDto} e materia prima a legendei: "peste 9.5 cornere in 4/7 meciuri acasa".
 * {@code egaluri}/{@code reprize} sunt {@code null} cand niciuna dintre echipe nu are istoric.
 */
public record StatisticiAvansateDto(
        PiataDto goluri,
        GgDto gg,
        PiataDto cornere,
        PiataDto faulturi,
        PiataDto cartonase,
        PiataDto suturi,
        PiataDto suturiPePoarta,
        EgaluriDto egaluri,
        ReprizeDto reprize,
        RezultatDto rezultat
) {

    /** O piata cu linii Over/Under si mediile fiecarei echipe pe ferestrele ei. */
    public record PiataDto(List<LinieDto> linii, MediiEchipaDto gazde, MediiEchipaDto oaspeti) {
    }

    /** O linie x.5: probabilitatea modelata pe meci + frecventele empirice ale fiecarei ferestre. */
    public record LinieDto(
            double linie,
            double probabilitate,
            FrecventaDto gazdeLocatie,
            FrecventaDto gazdeGeneral,
            FrecventaDto oaspetiLocatie,
            FrecventaDto oaspetiGeneral
    ) {
    }

    /** "In {@code reusite} din {@code total} meciuri" — {@code total} = 0 inseamna fara date. */
    public record FrecventaDto(int reusite, int total) {
    }

    /**
     * Mediile pe meci ale unei echipe pe piata: {@code proprie*} = doar evenimentele echipei,
     * {@code total*} = ale ambelor parti din meciurile ei; {@code null} = fara date.
     */
    public record MediiEchipaDto(
            Double proprieLocatie,
            Double totalLocatie,
            Double proprieGeneral,
            Double totalGeneral
    ) {
    }

    /** GG (ambele marcheaza): probabilitatea modelata + marcat/primit pe ferestrele de locatie. */
    public record GgDto(
            double probabilitate,
            FrecventaDto gazdeMarcat,
            FrecventaDto gazdePrimit,
            FrecventaDto oaspetiMarcat,
            FrecventaDto oaspetiPrimit
    ) {
    }

    /** Egal la pauza / egal la final, cu frecventele fiecarei echipe pe fereastra de locatie. */
    public record EgaluriDto(
            double egalPauza,
            double egalFinal,
            FrecventaDto pauzaGazde,
            FrecventaDto pauzaOaspeti,
            FrecventaDto finalGazde,
            FrecventaDto finalOaspeti
    ) {
    }

    /** Se marcheaza in repriza 1 / 2, cu frecventele fiecarei echipe pe fereastra de locatie. */
    public record ReprizeDto(
            double golRepriza1,
            double golRepriza2,
            FrecventaDto repriza1Gazde,
            FrecventaDto repriza1Oaspeti,
            FrecventaDto repriza2Gazde,
            FrecventaDto repriza2Oaspeti
    ) {
    }

    /**
     * Totalurile reale ale meciului pe fiecare piata, pentru a marca hit/miss fata de partea favorizata
     * de model. {@code null} la meciuri viitoare. Golurile sunt la 90 min (comparabile cu modelul);
     * campurile de repriza sunt {@code Boolean} nule cand scorul la pauza lipseste, iar totalurile de
     * count sunt {@code Integer} nule cand meciul n-are statistici colectate (ex. amicale internationale).
     */
    public record RezultatDto(
            int totalGoluri,
            boolean ambeleMarcheaza,
            boolean egalFinal,
            Boolean egalPauza,
            Boolean golRepriza1,
            Boolean golRepriza2,
            Integer totalCornere,
            Integer totalFaulturi,
            Integer totalCartonase,
            Integer totalSuturi,
            Integer totalSuturiPePoarta
    ) {
    }
}
