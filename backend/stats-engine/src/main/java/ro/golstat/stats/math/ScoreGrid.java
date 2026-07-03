package ro.golstat.stats.math;

/**
 * Matricea de probabilitate a scorurilor exacte cu corectia Dixon-Coles peste Poisson-ul
 * dublu independent. {@code X = golurile noastre ~ Poisson(lambdaFor)},
 * {@code Y = golurile primite ~ Poisson(lambdaAgainst)}.
 *
 * <p>Dixon-Coles corecteaza doar cele 4 scoruri mici (0-0, 0-1, 1-0, 1-1) printr-un factor
 * {@code τ}; {@code rho = 0} reduce exact la Poisson-ul independent. Corectia lasa marginalele
 * si suma matricei neschimbate — muta masa doar intre scorurile mici (afecteaza BTTS si linia 1.5,
 * NU liniile 2.5/3.5).
 *
 * <p>Nu e un {@code record}: incapsuleaza matricea interna in loc s-o expuna prin accesor.
 */
public final class ScoreGrid {

    private final double[][] p;

    private ScoreGrid(double[][] p) {
        this.p = p;
    }

    public static ScoreGrid dixonColes(double lambdaFor, double lambdaAgainst, double rho, int maxGoals) {
        if (lambdaFor < 0 || lambdaAgainst < 0) {
            throw new IllegalArgumentException("lambda trebuie >= 0");
        }
        if (maxGoals < 1) {
            throw new IllegalArgumentException("maxGoals trebuie >= 1, a fost " + maxGoals);
        }
        double safeRho = clampRho(lambdaFor, lambdaAgainst, rho);
        double[][] grid = new double[maxGoals + 1][maxGoals + 1];
        for (int x = 0; x <= maxGoals; x++) {
            double pForX = Poisson.pmf(lambdaFor, x);
            for (int y = 0; y <= maxGoals; y++) {
                grid[x][y] = tau(x, y, lambdaFor, lambdaAgainst, safeRho)
                        * pForX * Poisson.pmf(lambdaAgainst, y);
            }
        }
        return new ScoreGrid(grid);
    }

    /** Poisson dublu independent (rho = 0). */
    public static ScoreGrid independent(double lambdaFor, double lambdaAgainst, int maxGoals) {
        return dixonColes(lambdaFor, lambdaAgainst, 0.0, maxGoals);
    }

    public double exact(int x, int y) {
        return p[x][y];
    }

    /** Suma matricei (≈ 1 daca maxGoals acopera coada). */
    public double total() {
        double s = 0.0;
        for (double[] row : p) {
            for (double cell : row) {
                s += cell;
            }
        }
        return s;
    }

    /** P(total &gt; line): suma celulelor cu {@code x + y >= floor(line) + 1}. */
    public double probabilityOverTotal(double line) {
        int threshold = (int) Math.floor(line) + 1;
        double over = 0.0;
        for (int x = 0; x < p.length; x++) {
            for (int y = 0; y < p.length; y++) {
                if (x + y >= threshold) {
                    over += p[x][y];
                }
            }
        }
        return over;
    }

    /** P(ambele echipe marcheaza): suma celulelor cu {@code x >= 1 si y >= 1}. */
    public double btts() {
        double s = 0.0;
        for (int x = 1; x < p.length; x++) {
            for (int y = 1; y < p.length; y++) {
                s += p[x][y];
            }
        }
        return s;
    }

    /** 1X2 — gazda castiga: P(X &gt; Y). ({@code X} = golurile gazdei, {@code Y} ale oaspetilor.) */
    public double homeWin() {
        double s = 0.0;
        for (int x = 0; x < p.length; x++) {
            for (int y = 0; y < x; y++) {
                s += p[x][y];
            }
        }
        return s;
    }

    /** 1X2 — egal: P(X == Y). */
    public double draw() {
        double s = 0.0;
        for (int i = 0; i < p.length; i++) {
            s += p[i][i];
        }
        return s;
    }

    /** 1X2 — oaspetii castiga: P(X &lt; Y). */
    public double awayWin() {
        double s = 0.0;
        for (int x = 0; x < p.length; x++) {
            for (int y = x + 1; y < p.length; y++) {
                s += p[x][y];
            }
        }
        return s;
    }

    private static double tau(int x, int y, double lambdaFor, double lambdaAgainst, double rho) {
        if (x == 0 && y == 0) {
            return 1 - lambdaFor * lambdaAgainst * rho;
        }
        if (x == 0 && y == 1) {
            return 1 + lambdaFor * rho;
        }
        if (x == 1 && y == 0) {
            return 1 + lambdaAgainst * rho;
        }
        if (x == 1 && y == 1) {
            return 1 - rho;
        }
        return 1.0;
    }

    /** Clamp pe domeniul in care toate τ raman >= 0. */
    private static double clampRho(double lambdaFor, double lambdaAgainst, double rho) {
        if (lambdaFor <= 0 || lambdaAgainst <= 0) {
            return rho; // corectiile devin triviale, nimic de clamped
        }
        double lower = Math.max(-1.0 / lambdaFor, -1.0 / lambdaAgainst);
        double upper = Math.min(1.0 / (lambdaFor * lambdaAgainst), 1.0);
        return Math.max(lower, Math.min(upper, rho));
    }
}
