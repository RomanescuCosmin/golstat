package ro.golstat.common.dto;

public record TeamDto(
        Long id,
        String name,
        String code,
        String countryName,
        Integer founded,
        Boolean isNational,
        String logo,
        Long venueId
) {
}
