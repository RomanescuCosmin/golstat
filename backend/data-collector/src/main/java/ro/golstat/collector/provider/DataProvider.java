package ro.golstat.collector.provider;

import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.PlayerSezonDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;
import ro.golstat.common.dto.VenueDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstractizarea sursei de date. Intoarce DTO-uri din {@code common} (nu tipuri specifice
 * furnizorului), ca schimbarea de furnizor sa insemne o singura implementare noua in spatele
 * acestei interfete. Parametrii sunt de DOMENIU (liga, sezon, interval, meci) — fara concepte
 * API-Football (ex. {@code last=N}).
 *
 * <p>Deocamdata doar endpoint-urile necesare pietei de goluri; se extinde cand adaugam piete.
 */
public interface DataProvider {

    /** Fixtures dintr-o liga/sezon, in intervalul {@code [from, to]} (inclusiv). */
    List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to);

    /** Evenimentele unui meci (goluri, cartonase, schimbari...). */
    List<FixtureEventDto> fixtureEvents(long fixtureId);

    /** Statisticile de meci per echipa (posesie, suturi, cornere, faulturi, cartonase...). */
    List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId);

    /** Formatiile unui meci (startXI + rezerve + antrenor per echipa); apar aproape de kickoff. */
    List<FixtureLineupDto> fixtureLineups(long fixtureId);

    /**
     * Formatiile PROBABILE/anuntate ale unui meci VIITOR, cerute aproape de kickoff. Semantic e
     * acelasi endpoint ca {@link #fixtureLineups}, dar implementarea NU trebuie sa cache-uiasca
     * raspunsul gol pe termen lung: inainte de meci raspunsul e gol pana la anuntarea echipelor,
     * iar un gol inghetat in cache ar ascunde anuntul.
     */
    default List<FixtureLineupDto> upcomingFixtureLineups(long fixtureId) {
        return fixtureLineups(fixtureId);
    }

    /** Jucatorii indisponibili (accidentati/suspendati/incerti) dintr-o liga/sezon. */
    List<InjuryDto> injuries(long leagueId, int season);

    /**
     * Meciurile in DESFASURARE acum, din TOATE ligile (un singur apel la sursa), cu evenimentele inline
     * (gratis din {@code live=all}). Filtrarea pe ligile urmarite ramane la apelant. Se schimba la fiecare
     * poll → implementarea nu cache-uieste.
     */
    List<FixtureLiveDto> liveFixtures();

    /** Statisticile LIVE ale unui meci (TTL 0 — ocoleste cache-ul istoric); pentru refresh in timpul jocului. */
    default List<FixtureTeamStatsDto> liveFixtureStatistics(long fixtureId) {
        return List.of();
    }

    /**
     * Meciurile cu id-urile date, stare curenta (fara cache). Pentru „finalizarea" meciurilor care tocmai
     * au iesit din {@code live=all}: le luam scorul/statusul final (FT) o singura data, altfel ar ramane
     * inghetate la ultimul snapshot live. Max ~20 id-uri per apel la sursa.
     */
    default List<FixtureDto> fixturesByIds(java.util.Collection<Long> fixtureIds) {
        return List.of();
    }

    /** Clasamentul unei ligi/sezon. */
    List<StandingDto> standings(long leagueId, int season);

    /** Echipele dintr-o liga/sezon. */
    List<TeamDto> teams(long leagueId, int season);

    /** Statisticile de sezon ale unei echipe (pagina echipei). */
    default List<TeamSeasonStatsDto> teamStatistics(long leagueId, int season, long teamId) {
        return List.of();
    }

    /** Jucatorii unei echipe pe un sezon (profil + statistici, aduse impreuna). */
    default List<PlayerSezonDto> players(long teamId, int season) {
        return List.of();
    }

    /** Antrenorul curent al unei echipe (0 sau 1). */
    default List<CoachDto> coaches(long teamId) {
        return List.of();
    }

    // --- catalog (colectat rar; prerequisit FK pentru fixtures/standings) ---

    /** Competitiile (ligi/cupe) urmarite. */
    List<LeagueDto> leagues();

    /** Sezoanele unei competitii. */
    List<SeasonDto> seasons(long leagueId);

    /** Stadioanele. */
    List<VenueDto> venues();
}
