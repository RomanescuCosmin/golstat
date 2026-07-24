package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.stats.GoalAverage;
import ro.golstat.api.stats.LeagueSeason;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    /**
     * Ultimele meciuri TERMINALE ale unei echipe (acasa sau deplasare) dinaintea unei date,
     * cele mai recente primele. Numarul se limiteaza prin {@link Pageable}.
     */
    @Query("""
            select f from Fixture f
            where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
              and f.statusShort in :terminal
              and f.kickoff < :before
            order by f.kickoff desc
            """)
    List<Fixture> findRecentForTeam(@Param("teamId") long teamId,
                                    @Param("terminal") Collection<String> terminal,
                                    @Param("before") OffsetDateTime before,
                                    Pageable pageable);

    /**
     * Ca {@link #findRecentForTeam}, dar pentru MULTE echipe intr-un singur query — istoricul comun
     * al unei ferestre intregi de zile. Fara limita per echipa (taierea la ultimele N si filtrul pe
     * kickoff-ul fiecarui meci se fac in Java, altfel ar trebui un query per echipa).
     */
    @Query("""
            select f from Fixture f
            where (f.homeTeamId in :teamIds or f.awayTeamId in :teamIds)
              and f.statusShort in :terminal
              and f.kickoff < :before
            order by f.kickoff desc
            """)
    List<Fixture> findTerminalForTeams(@Param("teamIds") Collection<Long> teamIds,
                                       @Param("terminal") Collection<String> terminal,
                                       @Param("before") OffsetDateTime before);

    /**
     * Media golurilor marcate de gazde / de oaspeti pe o liga/sezon, doar din meciuri TERMINALE.
     * Preferam scorul la 90 min ({@code scoreFt}), altfel {@code goals} (aceeasi logica ca la mapare).
     * Fara meciuri terminale → ambele getter-e {@code null}.
     */
    @Query("""
            select avg(coalesce(f.scoreFtHome, f.goalsHome)) as avgGazde,
                   avg(coalesce(f.scoreFtAway, f.goalsAway)) as avgOaspeti
            from Fixture f
            where f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    GoalAverage avgGoals(@Param("leagueId") long leagueId,
                         @Param("season") int season,
                         @Param("terminal") Collection<String> terminal);

    /**
     * Ligile care au meciuri TERMINALE, fiecare cu cel mai recent sezon jucat.
     *
     * <p>Inlocuieste o lista alba hardcodata de competitii: pagina de statistici arata astfel exact
     * ce s-a colectat, iar o liga noua apare singura dupa backfill, fara modificari de cod.
     * Intr-un singur query — varianta "pentru fiecare liga, cauta sezonul cu date" facea cate un
     * apel per sezon per liga.
     */
    @Query("""
            select f.leagueId as leagueId, max(f.seasonYear) as seasonYear
            from Fixture f
            where f.statusShort in :terminal
            group by f.leagueId
            """)
    List<LeagueSeason> ligiCuMeciuriJucate(@Param("terminal") Collection<String> terminal);

    /**
     * Intalnirile directe TERMINALE dintre doua echipe (indiferent cine a fost gazda) dinaintea
     * unei date, cele mai recente primele. Numarul se limiteaza prin {@link Pageable}.
     */
    @Query("""
            select f from Fixture f
            where ((f.homeTeamId = :teamA and f.awayTeamId = :teamB)
                or (f.homeTeamId = :teamB and f.awayTeamId = :teamA))
              and f.statusShort in :terminal
              and f.kickoff < :before
            order by f.kickoff desc
            """)
    List<Fixture> findHeadToHead(@Param("teamA") long teamA,
                                 @Param("teamB") long teamB,
                                 @Param("terminal") Collection<String> terminal,
                                 @Param("before") OffsetDateTime before,
                                 Pageable pageable);

    /** Meciurile viitoare ({@code NS}) ale unei ligi intr-o fereastra, cele mai apropiate primele. */
    @Query("""
            select f from Fixture f
            where f.leagueId = :leagueId
              and f.statusShort = :status
              and f.kickoff >= :from
              and f.kickoff < :to
            order by f.kickoff asc
            """)
    List<Fixture> findUpcoming(@Param("leagueId") long leagueId,
                               @Param("status") String status,
                               @Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to);

    /** Meciurile viitoare ({@code NS}) din TOATE competitiile intr-o fereastra, cronologic. */
    @Query("""
            select f from Fixture f
            where f.statusShort = :status
              and f.kickoff >= :from
              and f.kickoff < :to
            order by f.kickoff asc
            """)
    List<Fixture> findUpcomingAll(@Param("status") String status,
                                  @Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to);

    /**
     * Exista vreun meci cu kickoff in fereastra data (orice status)? Poarta ieftina pentru bucla LIVE:
     * polim API-Football doar in preajma unui meci, altfel am arde requesturi 24/7 degeaba.
     */
    boolean existsByKickoffBetween(OffsetDateTime start, OffsetDateTime end);

    /** Meciurile in DESFASURARE acum (orice liga), cele mai apropiate de start primele. */
    @Query("""
            select f from Fixture f
            where f.statusShort in :inPlay
            order by f.kickoff asc
            """)
    List<Fixture> findLive(@Param("inPlay") Collection<String> inPlay);

    /** Toate meciurile dintr-o fereastra (o zi) din TOATE competitiile (orice status), cronologic. */
    @Query("""
            select f from Fixture f
            where f.kickoff >= :from
              and f.kickoff < :to
            order by f.kickoff asc
            """)
    List<Fixture> findByDayAllLeagues(@Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);

    /** Toate meciurile unei ligi intr-o fereastra de timp (orice status), cronologic. */
    @Query("""
            select f from Fixture f
            where f.leagueId = :leagueId
              and f.kickoff >= :from
              and f.kickoff < :to
            order by f.kickoff asc
            """)
    List<Fixture> findByDay(@Param("leagueId") long leagueId,
                            @Param("from") OffsetDateTime from,
                            @Param("to") OffsetDateTime to);

    /** Ultimele meciuri dintr-o liga/sezon cu un status dat (ex. TERMINALE) — pentru pagina de competitie. */
    List<Fixture> findByLeagueIdAndSeasonYearAndStatusShortInOrderByKickoffDesc(
            Long leagueId, Integer seasonYear, Collection<String> statusShort, Pageable pageable);

    /** Meciurile viitoare ({@code NS}) dintr-o liga/sezon, cele mai apropiate primele. */
    List<Fixture> findByLeagueIdAndSeasonYearAndStatusShortOrderByKickoffAsc(
            Long leagueId, Integer seasonYear, String statusShort, Pageable pageable);

    /** Toate meciurile unei ligi/sezon (orice status), cronologic — pentru schema fazelor eliminatorii. */
    List<Fixture> findByLeagueIdAndSeasonYearOrderByKickoffAsc(Long leagueId, Integer seasonYear);

    /**
     * Media golurilor marcate de o echipa pe o liga/sezon (meciuri TERMINALE), indiferent de locatie.
     * Fallback pentru {@code StatProcent} cand {@code team_season_stats} lipseste. {@code null} fara meciuri.
     */
    @Query("""
            select avg(case when f.homeTeamId = :teamId then coalesce(f.scoreFtHome, f.goalsHome)
                            else coalesce(f.scoreFtAway, f.goalsAway) end)
            from Fixture f
            where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
              and f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    Double avgGoalsForTeam(@Param("teamId") long teamId,
                           @Param("leagueId") long leagueId,
                           @Param("season") int season,
                           @Param("terminal") Collection<String> terminal);

    /** Sezoanele distincte in care echipa are meciuri (orice status), pentru selectorul de sezon. */
    @Query("""
            select distinct f.seasonYear from Fixture f
            where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
              and f.seasonYear is not null
            """)
    List<Integer> distinctSeasons(@Param("teamId") long teamId);

    /** Urmatoarele meciuri ({@code NS}) ale unei echipe de la {@code now} incolo, cele mai apropiate primele. */
    @Query("""
            select f from Fixture f
            where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
              and f.statusShort = :ns
              and f.kickoff >= :now
            order by f.kickoff asc
            """)
    List<Fixture> findNextForTeam(@Param("teamId") long teamId,
                                  @Param("ns") String ns,
                                  @Param("now") OffsetDateTime now,
                                  Pageable pageable);
}
