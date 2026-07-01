package ro.golstat.common.dto;

import java.time.LocalDate;

public record PlayerDto(
        Long id,
        String name,
        String firstname,
        String lastname,
        Integer age,
        LocalDate birthDate,
        String birthPlace,
        String birthCountry,
        String nationality,
        String height,
        String weight,
        Boolean isInjured,
        String photo
) {
}
