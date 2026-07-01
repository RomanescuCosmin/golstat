package ro.golstat.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coach")
@Getter
@Setter
@NoArgsConstructor
public class Coach {

    @Id
    private Long id;
    private String name;
    private String firstname;
    private String lastname;
    private Integer age;
    private String nationality;
    private String photo;
}
