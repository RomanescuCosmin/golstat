package ro.golstat.common.dto;

public record VenueDto(
        Long id,
        String name,
        String address,
        String city,
        String countryName,
        Integer capacity,
        String surface,
        String image
) {
}
