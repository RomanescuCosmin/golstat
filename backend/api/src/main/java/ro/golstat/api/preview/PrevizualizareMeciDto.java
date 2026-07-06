package ro.golstat.api.preview;

import ro.golstat.api.prediction.PredictieMeciDto;

import java.util.List;

/**
 * Previzualizarea unui meci viitor: predictie de goluri + forma ambelor echipe (pe locatie si
 * generala) + intalniri directe + analiza pe piete ({@link StatisticiAvansateDto}) + statisticile
 * cheie + echipa de start ({@code null} pana exista macar o formatie per echipa).
 */
public record PrevizualizareMeciDto(
        PredictieMeciDto predictie,
        FormaEchipaDto formaGazde,
        FormaEchipaDto formaOaspeti,
        List<IntalnireDirectaDto> intalniriDirecte,
        StatisticiAvansateDto statistici,
        StatisticiCheieDto statisticiCheie,
        EchipaDeStartDto echipeDeStart
) {
}
