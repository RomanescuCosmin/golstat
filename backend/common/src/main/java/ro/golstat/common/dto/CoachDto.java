package ro.golstat.common.dto;

public record CoachDto(
        Long id,
        String name,
        String firstname,
        String lastname,
        Integer age,
        String nationality,
        String photo
) {
}
