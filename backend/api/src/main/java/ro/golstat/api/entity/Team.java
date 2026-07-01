package ro.golstat.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    private Long id;
    private String name;
    private String code;
    private String countryName;
    private Integer founded;
    private Boolean isNational;
    private String logo;
    private Long venueId;
}
