package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsHistoryServiceTest {

    @Mock FixtureRepository fixtures;
    @Mock FixtureTeamStatsRepository teamStats;
    @InjectMocks StatsHistoryService service;

    private static Fixture fixture(long id, long home, long away) {
        Fixture f = new Fixture();
        f.setId(id);
        f.setHomeTeamId(home);
        f.setAwayTeamId(away);
        f.setKickoff(OffsetDateTime.parse("2024-08-10T18:00:00Z"));
        return f;
    }

    private static FixtureTeamStats stats(long fixtureId, long teamId, Integer cornere, Integer faulturi,
                                          Integer galbene, Integer rosii) {
        FixtureTeamStats s = new FixtureTeamStats();
        s.setFixtureId(fixtureId);
        s.setTeamId(teamId);
        s.setCornerKicks(cornere);
        s.setFouls(faulturi);
        s.setYellowCards(galbene);
        s.setRedCards(rosii);
        return s;
    }

    @Test
    void mapeazaCountForSiCountAgainst_dinPerspectivaEchipei() {
        // meciul 100: echipa 1 gazda; meciul 101: echipa 1 oaspete
        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any()))
                .thenReturn(List.of(fixture(100, 1, 2), fixture(101, 3, 1)));
        when(teamStats.findByFixtureIdIn(List.of(100L, 101L))).thenReturn(List.of(
                stats(100, 1, 7, 12, 2, 0),
                stats(100, 2, 4, 15, 3, 1),
                stats(101, 3, 5, 10, 1, null),
                stats(101, 1, 6, 14, 2, 0)));

        IstoricCounturi istoric = service.istoric(1, OffsetDateTime.parse("2024-09-01T00:00:00Z"), 10);

        assertEquals(2, istoric.cornere().size());
        EventCountSample acasa = istoric.cornere().get(0);
        assertTrue(acasa.home());
        assertEquals(7, acasa.countFor());
        assertEquals(4, acasa.countAgainst());
        assertEquals(LocalDate.of(2024, 8, 10), acasa.date());
        assertNull(acasa.opponentRank());
        EventCountSample deplasare = istoric.cornere().get(1);
        assertFalse(deplasare.home());
        assertEquals(6, deplasare.countFor());
        assertEquals(5, deplasare.countAgainst());
        // faulturi din aceleasi randuri
        assertEquals(12, istoric.faulturi().get(0).countFor());
        assertEquals(15, istoric.faulturi().get(0).countAgainst());
        // cartonase = galbene + rosii; rosii null langa galbene = 0
        assertEquals(2, istoric.cartonase().get(0).countFor());
        assertEquals(4, istoric.cartonase().get(0).countAgainst());
        assertEquals(1, istoric.cartonase().get(1).countAgainst());
        // randurile proprii, in ordinea meciurilor
        assertEquals(List.of(1L, 1L),
                istoric.statisticiEchipa().stream().map(FixtureTeamStats::getTeamId).toList());
        assertEquals(List.of(100L, 101L),
                istoric.statisticiEchipa().stream().map(FixtureTeamStats::getFixtureId).toList());
    }

    @Test
    void meciFaraStatistici_esteSarit() {
        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any()))
                .thenReturn(List.of(fixture(100, 1, 2), fixture(101, 1, 3)));
        // meciul 101 are doar randul echipei, fara adversar → sarit complet
        when(teamStats.findByFixtureIdIn(any())).thenReturn(List.of(
                stats(100, 1, 7, 12, 2, 0),
                stats(100, 2, 4, 15, 3, 0),
                stats(101, 1, 5, 10, 1, 0)));

        IstoricCounturi istoric = service.istoric(1, OffsetDateTime.now(), 10);

        assertEquals(1, istoric.cornere().size());
        assertEquals(1, istoric.statisticiEchipa().size());
        assertEquals(100L, istoric.statisticiEchipa().get(0).getFixtureId());
    }

    @Test
    void campLipsa_sareDoarPiataRespectiva() {
        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any()))
                .thenReturn(List.of(fixture(100, 1, 2)));
        // fara cornere la adversar si fara niciun cartonas raportat la echipa → doar faulturi raman
        when(teamStats.findByFixtureIdIn(any())).thenReturn(List.of(
                stats(100, 1, 7, 12, null, null),
                stats(100, 2, null, 15, 3, 0)));

        IstoricCounturi istoric = service.istoric(1, OffsetDateTime.now(), 10);

        assertEquals(0, istoric.cornere().size());
        assertEquals(0, istoric.cartonase().size());
        assertEquals(1, istoric.faulturi().size());
        assertEquals(1, istoric.statisticiEchipa().size());
    }

    @Test
    void faraMeciuri_istoricGol_faraQueryPeStatistici() {
        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any())).thenReturn(List.of());

        IstoricCounturi istoric = service.istoric(1, OffsetDateTime.now(), 10);

        assertEquals(IstoricCounturi.gol(), istoric);
        verify(teamStats, never()).findByFixtureIdIn(any());
    }
}
