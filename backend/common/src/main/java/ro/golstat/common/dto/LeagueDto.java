package ro.golstat.common.dto;

public record LeagueDto(
        Long id,
        String name,
        String type,
        String logo,
        String countryName
) {
}
