package ro.golstat.api.stats;

/** Golurile unei echipe pe o parte (gazda sau oaspete) din meciurile terminate: sume + numar de meciuri. */
public interface GoluriEchipaRow {

    Long getTeamId();

    Long getMarcate();

    Long getPrimite();

    Long getMeciuri();
}
