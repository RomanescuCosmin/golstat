package ro.golstat.common;

public final class GolstatConstants {

    private GolstatConstants() {
    }

    public static final class KafkaTopics {

        private KafkaTopics() {
        }

        public static final String COUNTRIES = "golstat.countries";
        public static final String VENUES = "golstat.venues";
        public static final String LEAGUES = "golstat.leagues";
        public static final String SEASONS = "golstat.seasons";
        public static final String TEAMS = "golstat.teams";
        public static final String PLAYERS = "golstat.players";
        public static final String COACHES = "golstat.coaches";
        public static final String TEAM_SEASON_STATS = "golstat.team-season-stats";
        public static final String PLAYER_SEASON_STATS = "golstat.player-season-stats";
        public static final String STANDINGS = "golstat.standings";
        public static final String FIXTURES = "golstat.fixtures";
        public static final String FIXTURE_EVENTS = "golstat.fixture-events";
        public static final String FIXTURE_TEAM_STATS = "golstat.fixture-team-stats";
        public static final String FIXTURE_LINEUPS = "golstat.fixture-lineups";
        public static final String FIXTURE_PLAYER_STATS = "golstat.fixture-player-stats";
        public static final String INJURIES = "golstat.injuries";
    }

    public static final class ApiFootball {

        private ApiFootball() {
        }

        public static final String COUNTRIES = "/countries";
        public static final String VENUES = "/venues";
        public static final String LEAGUES = "/leagues";
        public static final String TEAMS = "/teams";
        public static final String TEAMS_STATISTICS = "/teams/statistics";
        public static final String PLAYERS = "/players";
        public static final String COACHS = "/coachs";
        public static final String STANDINGS = "/standings";
        public static final String FIXTURES = "/fixtures";
        public static final String FIXTURES_EVENTS = "/fixtures/events";
        public static final String FIXTURES_STATISTICS = "/fixtures/statistics";
        public static final String FIXTURES_LINEUPS = "/fixtures/lineups";
        public static final String FIXTURES_PLAYERS = "/fixtures/players";
        public static final String INJURIES = "/injuries";
    }

    public static final class Piata {

        private Piata() {
        }

        public static final String CORNERE = "CORNERE";
        public static final String GOLURI = "GOLURI";
        public static final String FAULTURI = "FAULTURI";
        public static final String CARTONASE = "CARTONASE";
        public static final String MARCATORI = "MARCATORI";
    }

    public static final class EventType {

        private EventType() {
        }

        public static final String GOAL = "Goal";
        public static final String CARD = "Card";
        public static final String SUBST = "subst";
        public static final String VAR = "Var";
    }

    public static final class FixtureStatus {

        private FixtureStatus() {
        }

        public static final String NOT_STARTED = "NS";
        public static final String FIRST_HALF = "1H";
        public static final String HALF_TIME = "HT";
        public static final String SECOND_HALF = "2H";
        public static final String EXTRA_TIME = "ET";
        public static final String PENALTY = "P";
        public static final String FINISHED = "FT";
        public static final String FINISHED_AET = "AET";
        public static final String FINISHED_PEN = "PEN";
        public static final String POSTPONED = "PST";
        public static final String CANCELLED = "CANC";
    }
}
