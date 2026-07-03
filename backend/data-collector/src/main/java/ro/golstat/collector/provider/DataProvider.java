package ro.golstat.collector.provider;

import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
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

    /** Jucatorii indisponibili (accidentati/suspendati/incerti) dintr-o liga/sezon. */
    List<InjuryDto> injuries(long leagueId, int season);

    /**
     * Meciurile in DESFASURARE acum, din TOATE ligile (un singur apel la sursa). Filtrarea pe ligile
     * urmarite ramane in sarcina apelantului. Se schimba la fiecare poll → implementarea nu cache-uieste.
     */
    List<FixtureDto> liveFixtures();

    /** Clasamentul unei ligi/sezon. */
    List<StandingDto> standings(long leagueId, int season);

    /** Echipele dintr-o liga/sezon. */
    List<TeamDto> teams(long leagueId, int season);

    // --- catalog (colectat rar; prerequisit FK pentru fixtures/standings) ---

    /** Competitiile (ligi/cupe) urmarite. */
    List<LeagueDto> leagues();

    /** Sezoanele unei competitii. */
    List<SeasonDto> seasons(long leagueId);

    /** Stadioanele. */
    List<VenueDto> venues();
}
