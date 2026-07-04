package ro.golstat.common.dto;

import java.util.List;

/**
 * Un jucator cu profilul lui si statisticile de sezon aduse impreuna dintr-un singur apel
 * ({@code /players}); nu le desparti in doua apeluri. {@code statistici} poate avea mai multe
 * intrari (competitii diferite in acelasi sezon).
 */
public record PlayerSezonDto(PlayerDto profil, List<PlayerSeasonStatsDto> statistici) {
}
