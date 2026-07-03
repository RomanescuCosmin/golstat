package ro.golstat.api.prediction;

/** Nu exista predictie pentru un meci (inexistent sau nu e viitor). Mapata la 404 in advice. */
public class PredictionNotFoundException extends RuntimeException {

    public PredictionNotFoundException(long fixtureId) {
        super("Fara predictie pentru meciul " + fixtureId + " (inexistent sau nu e viitor)");
    }
}
