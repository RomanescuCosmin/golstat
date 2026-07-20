package ro.golstat.api.stats;

/**
 * Ca {@link RefereeCardAverage}, dar cu numele arbitrului — pentru agregarea in BLOC pe mai multi
 * arbitri deodata ({@code group by referee}), cand imbogatim o fereastra intreaga de zile.
 */
public interface RefereeCardAverageRow extends RefereeCardAverage {

    String getReferee();
}
