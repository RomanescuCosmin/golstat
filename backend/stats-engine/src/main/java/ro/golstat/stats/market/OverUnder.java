package ro.golstat.stats.market;

/**
 * O linie de pariere x.5 pentru un total de meci (goluri, cornere, faulturi, cartonase):
 * probabilitatea ca totalul sa fie peste (over) sau sub (under) linie.
 * {@code overRate + underRate == 1} — liniile x.5 nu au egalitate exact pe linie.
 * Tip partajat de toate pietele de tip Over/Under.
 */
public record OverUnder(double line, double overRate, double underRate) {
}
