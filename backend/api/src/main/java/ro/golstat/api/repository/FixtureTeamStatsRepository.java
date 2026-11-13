package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.stats.CountAverage;
import ro.golstat.api.stats.RefereeCardAverage;
import ro.golstat.api.stats.RefereeCardAverageRow;
import ro.golstat.api.stats.RefereeProfile;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FixtureTeamStatsRepository extends JpaRepository<FixtureTeamStats, FixtureTeamStats.Pk> {

    List<FixtureTeamStats> findByFixtureIdIn(Collection<Long> fixtureIds);

    /** Statisticile pe meci ale unei echipe intr-o liga/sezon (meciuri TERMINALE) — mediate in Java pentru barele de sezon. */
    @Query("""
            select s from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where s.teamId = :teamId
              and f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    List<FixtureTeamStats> findForTeamSeason(@Param("teamId") long teamId,
                                             @Param("leagueId") long leagueId,
                                             @Param("season") int season,
                                             @Param("terminal") Collection<String> terminal);

    /**
     * Mediile PE ECHIPA de cornere/faulturi/cartonase/suturi pe o liga/sezon, doar din meciuri
     * TERMINALE cu statistici colectate. Rosiile lipsa langa galbene inseamna 0; randurile fara
     * galbene si fara rosii nu intra la cartonase; unde avem {@code cartonase} (din incidente) il folosim
     * pe el, ca in {@code StatsHistoryService.totalCartonase}.
     * Fara randuri → toate getter-ele {@code null}.
     */
    @Query("""
            select avg(s.cornerKicks) as avgCornere,
                   avg(s.fouls) as avgFaulturi,
                   avg(case when s.cartonase is not null then s.cartonase
                            when s.yellowCards is not null or s.redCards is not null
                            then coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) end) as avgCartonase,
                   avg(s.shotsTotal) as avgSuturi,
                   avg(s.shotsOnGoal) as avgSuturiPePoarta,
                   avg(case when s.teamId = f.homeTeamId then s.cornerKicks end) as avgCornereGazde,
                   avg(case when s.teamId = f.awayTeamId then s.cornerKicks end) as avgCornereOaspeti,
                   avg(case when s.teamId = f.homeTeamId then s.fouls end) as avgFaulturiGazde,
                   avg(case when s.teamId = f.awayTeamId then s.fouls end) as avgFaulturiOaspeti,
                   avg(case when s.teamId = f.homeTeamId then (case when s.cartonase is not null then s.cartonase
                            when s.yellowCards is not null or s.redCards is not null
                            then coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) end) end) as avgCartonaseGazde,
                   avg(case when s.teamId = f.awayTeamId then (case when s.cartonase is not null then s.cartonase
                            when s.yellowCards is not null or s.redCards is not null
                            then coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) end) end) as avgCartonaseOaspeti
            from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
              and f.kickoff < :before
            """)
    CountAverage avgCounts(@Param("leagueId") long leagueId,
                           @Param("season") int season,
                           @Param("terminal") Collection<String> terminal,
                           @Param("before") OffsetDateTime before);

    /** Profilul arbitrului (cartonase dupa regula pietelor, faulturi, eliminari) din meciurile terminate. */
    @Query("""
            select count(distinct s.fixtureId) as matches,
                   2 * avg(case when s.cartonase is not null then s.cartonase
                            when s.yellowCards is not null or s.redCards is not null
                            then coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) end) as avgCards,
                   2 * avg(s.fouls) as avgFouls,
                   sum(coalesce(s.redCards, 0)) as reds
            from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where f.referee = :referee
              and f.statusShort in :terminal
              and f.kickoff < :before
            """)
    RefereeProfile refereeProfile(@Param("referee") String referee,
                                  @Param("terminal") Collection<String> terminal,
                                  @Param("before") OffsetDateTime before);

    /**
     * Meciurile la care galbene + rosii din statisticile furnizorului depasesc cartonasele din incidente:
     * de regula un al doilea galben pierdut la import (furnizorul il numara galben + galben + rosu). Meciurile
     * care au deja al doilea galben in incidente sunt corecte (diferenta e chiar regula pietelor) si se sar.
     */
    @Query("""
            select distinct s.fixtureId from FixtureTeamStats s
            where s.fixtureId in :fixtureIds
              and s.cartonase is not null
              and coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) > s.cartonase
              and not exists (select 1 from FixtureEvent e
                              where e.fixtureId = s.fixtureId and e.detail = 'Second Yellow card')
            """)
    Set<Long> meciuriCuCartonaseDiferite(@Param("fixtureIds") Collection<Long> fixtureIds);

    /**
     * Istoricul de cartonase al unui arbitru, doar din meciuri TERMINALE cu statistici colectate.
     * Fiecare meci are 2 randuri (unul per echipa), deci totalul pe meci = 2 × media pe rand.
     * Arbitru fara meciuri → {@code getAvgCards()} {@code null} si {@code getMatches()} 0.
     */
    @Query("""
            select 2 * avg(case when s.cartonase is not null then s.cartonase
                            when s.yellowCards is not null or s.redCards is not null
                            then coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) end) as avgCards,
                   count(distinct s.fixtureId) as matches
            from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where f.referee = :referee
              and f.statusShort in :terminal
              and f.kickoff < :before
            """)
    RefereeCardAverage refereeCardAverage(@Param("referee") String referee,
                                          @Param("terminal") Collection<String> terminal,
                                          @Param("before") OffsetDateTime before);

    /**
     * Ca {@link #refereeCardAverage}, dar pentru mai multi arbitri intr-un singur query. Arbitrii
     * fara meciuri terminale cu statistici pur si simplu lipsesc din rezultat (apelantul cade pe
     * factorul neutru).
     */
    @Query("""
            select f.referee as referee,
                   2 * avg(case when s.cartonase is not null then s.cartonase
                            when s.yellowCards is not null or s.redCards is not null
                            then coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0) end) as avgCards,
                   count(distinct s.fixtureId) as matches
            from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where f.referee in :arbitri
              and f.statusShort in :terminal
              and f.kickoff < :before
            group by f.referee
            """)
    List<RefereeCardAverageRow> refereeCardAverages(@Param("arbitri") Collection<String> arbitri,
                                                    @Param("terminal") Collection<String> terminal,
                                                    @Param("before") OffsetDateTime before);
}
