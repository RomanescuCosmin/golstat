package ro.golstat.api.prediction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.api.stats.MatchHistoryService;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.match.MatchContext;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    private static final OffsetDateTime KICKOFF = OffsetDateTime.parse("2025-08-16T14:00:00Z");

    @Mock FixtureRepository fixtures;
    @Mock TeamRepository teams;
    @Mock MatchHistoryService history;
    @Mock LeagueAverageService leagueAverages;
    @InjectMocks PredictionService service;

    private static MatchSample sample(int day, boolean home, int gf, int ga) {
        return new MatchSample(LocalDate.of(2025, 5, day), home, gf, ga, 0, 0, null);
    }

    private static Team team(long id, String nume, String logo) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        t.setLogo(logo);
        return t;
    }

    private static Fixture nsFixture() {
        Fixture f = new Fixture();
        f.setId(100L);
        f.setHomeTeamId(10L);
        f.setAwayTeamId(20L);
        f.setLeagueId(39L);
        f.setSeasonYear(2025);
        f.setKickoff(KICKOFF);
        f.setStatusShort(GolstatConstants.FixtureStatus.NOT_STARTED);
        return f;
    }

    @Test
    void buildContext_enoughLocationMatches_usesLocationWindowsAndSplitLeagueAverages() {
        List<MatchSample> home = List.of(sample(1, true, 2, 1), sample(2, true, 1, 1),
                sample(3, true, 3, 0), sample(4, true, 2, 2));
        List<MatchSample> away = List.of(sample(1, false, 1, 2), sample(2, false, 0, 0),
                sample(3, false, 2, 1), sample(4, false, 1, 1));

        MatchContext ctx = PredictionService.buildContext(home, away, new LeagueAverages(1.6, 1.2));

        assertEquals(1.6, ctx.mediaLigaGazde(), 1e-9);
        assertEquals(1.2, ctx.mediaLigaOaspeti(), 1e-9);
        assertEquals(4, ctx.gazdaAcasa().sampleSize());
        assertEquals(4, ctx.oaspetiDeplasare().sampleSize());
    }

    @Test
    void buildContext_tooFewLocationMatches_fallsBackToNeutralGround() {
        // gazda are doar 2 meciuri ACASA (+ 3 in deplasare) → sub prag → teren neutru
        List<MatchSample> home = List.of(sample(1, true, 2, 1), sample(2, true, 1, 0),
                sample(3, false, 0, 2), sample(4, false, 1, 1), sample(5, false, 2, 2));
        List<MatchSample> away = List.of(sample(1, false, 1, 1), sample(2, false, 0, 0),
                sample(3, false, 2, 1), sample(4, false, 1, 2));

        MatchContext ctx = PredictionService.buildContext(home, away, new LeagueAverages(1.6, 1.2));

        double neutru = (1.6 + 1.2) / 2.0;
        assertEquals(neutru, ctx.mediaLigaGazde(), 1e-9);
        assertEquals(neutru, ctx.mediaLigaOaspeti(), 1e-9);
        assertEquals(5, ctx.gazdaAcasa().sampleSize(), "fara filtru de locatie → toate meciurile");
    }

    @Test
    void predict_nsFixture_wiresHistoryAveragesAndModel() {
        when(fixtures.findById(100L)).thenReturn(Optional.of(nsFixture()));
        when(teams.findAllById(List.of(10L, 20L))).thenReturn(List.of(
                team(10L, "FC Gazde", "http://logo/10.png"), team(20L, "FC Oaspeti", "http://logo/20.png")));
        when(history.lastMatches(eq(10L), any(), anyInt())).thenReturn(List.of(
                sample(1, true, 2, 1), sample(2, true, 3, 1), sample(3, true, 2, 0)));
        when(history.lastMatches(eq(20L), any(), anyInt())).thenReturn(List.of(
                sample(1, false, 1, 1), sample(2, false, 0, 2), sample(3, false, 1, 2)));
        when(leagueAverages.averages(39L, 2025)).thenReturn(new LeagueAverages(1.6, 1.2));

        Optional<PredictieMeciDto> result = service.predict(100L);

        assertTrue(result.isPresent());
        PredictieMeciDto dto = result.get();
        assertEquals(100L, dto.fixtureId());
        assertEquals(new PredictieMeciDto.EchipaDto(10L, "FC Gazde", "http://logo/10.png"), dto.echipaGazde());
        assertEquals(new PredictieMeciDto.EchipaDto(20L, "FC Oaspeti", "http://logo/20.png"), dto.echipaOaspeti());
        assertTrue(dto.lambdaGazde() > 0 && dto.lambdaOaspeti() > 0);
        assertTrue(dto.lambdaGazde() > dto.lambdaOaspeti(), "gazda ataca mai bine");
        double suma1x2 = dto.gazde().procent() + dto.egal().procent() + dto.oaspeti().procent();
        assertEquals(100.0, suma1x2, 0.5);
    }

    @Test
    void predict_missingTeamInDb_returnsTeamWithIdOnly() {
        when(fixtures.findById(100L)).thenReturn(Optional.of(nsFixture()));
        // doar gazdele exista in DB
        when(teams.findAllById(List.of(10L, 20L)))
                .thenReturn(List.of(team(10L, "FC Gazde", "http://logo/10.png")));
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(leagueAverages.averages(39L, 2025)).thenReturn(new LeagueAverages(1.6, 1.2));

        PredictieMeciDto dto = service.predict(100L).orElseThrow();

        assertEquals(new PredictieMeciDto.EchipaDto(10L, "FC Gazde", "http://logo/10.png"), dto.echipaGazde());
        assertEquals(new PredictieMeciDto.EchipaDto(20L, null, null), dto.echipaOaspeti());
    }

    @Test
    void predict_finishedFixture_attachesRealResultAt90Min() {
        Fixture finished = nsFixture();
        finished.setStatusShort(GolstatConstants.FixtureStatus.FINISHED);
        finished.setGoalsHome(3);
        finished.setGoalsAway(1);
        finished.setScoreFtHome(2);
        finished.setScoreFtAway(1);
        when(fixtures.findById(100L)).thenReturn(Optional.of(finished));
        when(teams.findAllById(List.of(10L, 20L))).thenReturn(List.of(
                team(10L, "FC Gazde", "http://logo/10.png"), team(20L, "FC Oaspeti", "http://logo/20.png")));
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(leagueAverages.averages(39L, 2025)).thenReturn(new LeagueAverages(1.6, 1.2));

        PredictieMeciDto dto = service.predict(100L).orElseThrow();

        // prefera scoreFt (90 min), nu goals (care ar include prelungirile)
        assertEquals(new PredictieMeciDto.RezultatDto(2, 1, GolstatConstants.FixtureStatus.FINISHED),
                dto.rezultat());
    }

    @Test
    void predict_nsFixture_hasNoResult() {
        when(fixtures.findById(100L)).thenReturn(Optional.of(nsFixture()));
        when(teams.findAllById(List.of(10L, 20L))).thenReturn(List.of());
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(leagueAverages.averages(39L, 2025)).thenReturn(new LeagueAverages(1.6, 1.2));

        assertNull(service.predict(100L).orElseThrow().rezultat());
    }

    @Test
    void predict_inPlayFixture_isEmpty() {
        Fixture live = nsFixture();
        live.setStatusShort(GolstatConstants.FixtureStatus.SECOND_HALF);
        when(fixtures.findById(100L)).thenReturn(Optional.of(live));

        assertFalse(service.predict(100L).isPresent());
    }

    @Test
    void predict_missingFixture_isEmpty() {
        when(fixtures.findById(anyLong())).thenReturn(Optional.empty());
        assertFalse(service.predict(999L).isPresent());
    }
}
