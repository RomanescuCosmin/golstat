package ro.golstat.api.stats;

import ro.golstat.api.preview.StatisticiAvansateBuilder.FerestreEchipa;
import ro.golstat.stats.model.MatchSample;

import java.util.List;

/**
 * Ferestrele de analiza ale celor doua echipe ale unui meci: gazdele ACASA, oaspetii in DEPLASARE.
 *
 * <p>Pe langa ferestrele taiate pe piete, ducem si istoricul BRUT al fiecarei echipe — modelul de
 * goluri (1X2, din care iese P(egal)) foloseste alta marime de fereastra decat pietele, iar fara
 * istoricul brut ar trebui reincarcat din baza, adica exact N+1-ul pe care il evitam.
 */
public record FerestreMeci(
        FerestreEchipa gazde,
        FerestreEchipa oaspeti,
        List<MatchSample> istoricGazde,
        List<MatchSample> istoricOaspeti
) {
}
