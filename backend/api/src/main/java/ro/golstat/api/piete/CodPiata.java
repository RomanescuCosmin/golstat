package ro.golstat.api.piete;

/**
 * Vocabularul de piete al listei pe zile. E vocabular de REST (numele ajung ca atare in JSON si in
 * filtrele din interfata), de aceea sta aici si nu in {@code GolstatConstants.Piata}, care descrie
 * categoriile de domeniu ale colectorului.
 *
 * <p>{@code areLinie} = piata se exprima pe o linie x.5 (peste 2.5 goluri); restul sunt piete
 * binare (GG, egal la pauza).
 */
public enum CodPiata {

    GOLURI_PESTE(true),
    GOLURI_SUB(true),
    GG(false),
    NG(false),
    CORNERE_PESTE(true),
    FAULTURI_PESTE(true),
    CARTONASE_PESTE(true),
    EGAL_PAUZA(false),
    EGAL_FINAL(false);

    private final boolean areLinie;

    CodPiata(boolean areLinie) {
        this.areLinie = areLinie;
    }

    public boolean areLinie() {
        return areLinie;
    }
}
