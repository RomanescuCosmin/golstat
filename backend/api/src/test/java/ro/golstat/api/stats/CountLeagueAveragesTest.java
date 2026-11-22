package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import ro.golstat.api.stats.CountLeagueAverages.PeLocatie;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountLeagueAveragesTest {

    @Test
    void neutre_faraAvantajulTerenului_aceeasiMediePentruAmbeleEchipe() {
        CountLeagueAverages medii = new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0,
                new PeLocatie(5.6, 4.4), new PeLocatie(11.0, 13.0), new PeLocatie(1.8, 2.2),
                new PeLocatie(2.5, 2.1), new PeLocatie(3.1, 2.3));

        CountLeagueAverages n = medii.neutre();

        assertEquals(new PeLocatie(5.0, 5.0), n.cornere());
        assertEquals(new PeLocatie(12.0, 12.0), n.faulturi());
        assertEquals(new PeLocatie(2.0, 2.0), n.cartonase());
        assertEquals(2.3, n.cornereRepriza1().gazde(), 1e-12);
        assertEquals(2.3, n.cornereRepriza1().oaspeti(), 1e-12);
        assertEquals(2.7, n.cornereRepriza2().gazde(), 1e-12);
        // totalurile pe meci raman neschimbate
        assertEquals(10.0, n.cornerePeMeci());
        assertEquals(4.0, n.cartonasePeMeci());
    }
}
