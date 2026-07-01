package ro.golstat.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venue")
@Getter
@Setter
@NoArgsConstructor
public class Venue {

    @Id
    private Long id;
    private String name;
    private String address;
    private String city;
    private String countryName;
    private Integer capacity;
    private String surface;
    private String image;
}
