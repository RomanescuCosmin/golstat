package ro.golstat.api.preview;

import ro.golstat.api.prediction.PredictieMeciDto;
import ro.golstat.stats.market.OverUnder;

import java.util.List;

/**
 * Previzualizarea unui meci viitor: predictie de goluri + forma ambelor echipe + intalniri directe
 * + liniile Over/Under pe pietele numarabile (totalul meciului) + statisticile cheie + echipa de
 * start ({@code null} pana se anunta formatiile, aproape de meci).
 */
public record PrevizualizareMeciDto(
        PredictieMeciDto predictie,
        FormaEchipaDto formaGazde,
        FormaEchipaDto formaOaspeti,
        List<IntalnireDirectaDto> intalniriDirecte,
        List<OverUnder> cornere,
        List<OverUnder> faulturi,
        List<OverUnder> cartonase,
        StatisticiCheieDto statisticiCheie,
        EchipaDeStartDto echipeDeStart
) {
}
