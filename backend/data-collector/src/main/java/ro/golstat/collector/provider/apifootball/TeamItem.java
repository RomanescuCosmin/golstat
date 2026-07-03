package ro.golstat.collector.provider.apifootball;

/** Un element din {@code /teams}: echipa si stadionul ei. */
public record TeamItem(Team team, Venue venue) {

    public record Team(Long id, String name, String code, String country,
                       Integer founded, Boolean national, String logo) {
    }

    public record Venue(Long id) {
    }
}
