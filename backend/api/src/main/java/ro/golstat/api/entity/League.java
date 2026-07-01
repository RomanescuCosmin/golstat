package ro.golstat.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "league")
@Getter
@Setter
@NoArgsConstructor
public class League {

    @Id
    private Long id;
    private String name;
    private String type;
    private String logo;
    private String countryName;
}
