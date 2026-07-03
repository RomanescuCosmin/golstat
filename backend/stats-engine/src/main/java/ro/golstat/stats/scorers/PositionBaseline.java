package ro.golstat.stats.scorers;

/**
 * Rate de referinta (goluri/90) pe pozitie: tinta shrinkage-ului pentru jucatorii cu putine
 * minute jucate. Valorile sunt placeholder-e de recalibrat din date reale; {@code K_POZ} e
 * increderea shrinkage-ului (cate "meciuri echivalente" cantaresc cat baseline-ul).
 */
public final class PositionBaseline {

    public static final double K_POZ = 5;

    private PositionBaseline() {
    }

    public static double goalsPer90(Position position) {
        return switch (position) {
            case FORWARD -> 0.35;
            case MIDFIELDER -> 0.12;
            case DEFENDER -> 0.04;
            case GOALKEEPER -> 0.001;
        };
    }
}
