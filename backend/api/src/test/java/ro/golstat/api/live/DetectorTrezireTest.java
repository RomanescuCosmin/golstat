package ro.golstat.api.live;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DetectorTrezireTest {

    /** Ceas pe care il mutam manual, ca sa simulam sleep-ul. */
    private static final class CeasManual extends Clock {
        Instant acum = Instant.parse("2026-11-22T11:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return acum; }

        void avanseaza(Duration d) { acum = acum.plus(d); }
    }

    private final ActualizareProgram actualizare = mock(ActualizareProgram.class);
    private final CeasManual ceas = new CeasManual();
    private final DetectorTrezire detector = new DetectorTrezire(actualizare, ceas);

    @Test
    void bataiNormale_nimicDeRecuperat() {
        detector.bataie();
        ceas.avanseaza(Duration.ofSeconds(30));
        detector.bataie();
        ceas.avanseaza(Duration.ofSeconds(45));
        detector.bataie();

        verify(actualizare, never()).recupereaza(any());
    }

    @Test
    void dupaSleep_recupereazaDeLaUltimaBataieDinainteDePauza() {
        detector.bataie();
        Instant inainteDeSleep = ceas.acum;
        ceas.avanseaza(Duration.ofHours(5));
        detector.bataie();

        verify(actualizare).recupereaza(inainteDeSleep);
    }

    @Test
    void recuperareEsuata_detectorulMergeMaiDeparte() {
        doThrow(new IllegalStateException("retea")).when(actualizare).recupereaza(any());
        detector.bataie();
        ceas.avanseaza(Duration.ofHours(1));
        detector.bataie();
        ceas.avanseaza(Duration.ofSeconds(30));
        detector.bataie();

        verify(actualizare).recupereaza(any());
    }
}
