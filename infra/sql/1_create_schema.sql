-- =============================================================================
-- golstat — schema initiala (baseline)
-- =============================================================================
-- Oglinda normalizata a datelor din API-Football (api-sports.io).
--   * identificatorii de tabele/coloane sunt in engleza => mapeaza 1:1 pe JSON-ul API
--   * comentariile si termenii de domeniu sunt in romana
--   * Postgres simplu (fara hypertables Timescale deocamdata; le adaugam pe
--     tabelele chiar time-series — ex. istoric cote — cand ajungem acolo)
--
-- Ruleaza automat la prima pornire a containerului (montat in
-- /docker-entrypoint-initdb.d). Ordinea tabelelor respecta dependintele (FK).
-- =============================================================================
-- CREATE EXTENSION IF NOT EXISTS timescaledb;

SET client_min_messages = warning;

-- =============================================================================
-- 1. DIMENSIUNI / REFERINTE
-- =============================================================================

-- Tari (endpoint /countries). Nu au id numeric in API => cheia e numele.
CREATE TABLE country (
    name        TEXT PRIMARY KEY,           -- ex. "Romania"
    code        TEXT,                        -- ex. "RO" (poate lipsi: ex. "World")
    flag        TEXT,                        -- url drapel
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Stadioane (endpoint /venues).
CREATE TABLE venue (
    id           BIGINT PRIMARY KEY,         -- id venue din API
    name         TEXT NOT NULL,
    address      TEXT,
    city         TEXT,
    country_name TEXT REFERENCES country(name),
    capacity     INTEGER,
    surface      TEXT,                        -- ex. "grass"
    image        TEXT,                        -- url
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Competitii (endpoint /leagues). type = "League" sau "Cup".
CREATE TABLE league (
    id           BIGINT PRIMARY KEY,         -- id league din API
    name         TEXT NOT NULL,
    type         TEXT,                        -- "League" / "Cup"
    logo         TEXT,
    country_name TEXT REFERENCES country(name),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sezoanele fiecarei competitii (leagues[].seasons[]).
CREATE TABLE season (
    league_id       BIGINT NOT NULL REFERENCES league(id),
    year            INTEGER NOT NULL,        -- ex. 2024
    start_date      DATE,
    end_date        DATE,
    is_current      BOOLEAN NOT NULL DEFAULT false,
    -- flag-uri "coverage" din API (ce date sunt disponibile pe planul curent)
    has_events      BOOLEAN,
    has_lineups     BOOLEAN,
    has_statistics_fixtures BOOLEAN,          -- statistics.fixtures
    has_statistics_players  BOOLEAN,          -- statistics.players
    has_standings   BOOLEAN,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (league_id, year)
);

-- Echipe (endpoint /teams).
CREATE TABLE team (
    id          BIGINT PRIMARY KEY,          -- id team din API
    name        TEXT NOT NULL,
    code        TEXT,                         -- ex. "FCB"
    country_name TEXT REFERENCES country(name),
    founded     INTEGER,
    is_national BOOLEAN NOT NULL DEFAULT false,
    logo        TEXT,
    venue_id    BIGINT REFERENCES venue(id),  -- stadionul de casa
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Jucatori (endpoint /players — profil de baza; statisticile sunt separat).
CREATE TABLE player (
    id            BIGINT PRIMARY KEY,        -- id player din API
    name          TEXT,                       -- numele afisat
    firstname     TEXT,
    lastname      TEXT,
    age           INTEGER,
    birth_date    DATE,
    birth_place   TEXT,
    birth_country TEXT,
    nationality   TEXT,
    height        TEXT,                       -- ex. "180 cm" (asa vine din API)
    weight        TEXT,                       -- ex. "75 kg"
    is_injured    BOOLEAN,
    photo         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Antrenori (endpoint /coachs). Minimal — util pentru context lineup.
CREATE TABLE coach (
    id          BIGINT PRIMARY KEY,
    name        TEXT,
    firstname   TEXT,
    lastname    TEXT,
    age         INTEGER,
    nationality TEXT,
    photo       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =============================================================================
-- 2. STATISTICI AGREGATE PE SEZON (forma / medii — baza pentru lambda)
-- =============================================================================

-- Statistici echipa per competitie+sezon (endpoint /teams/statistics).
-- Sursa principala pentru "MediaLiga", forta atac/aparare, split acasa/deplasare.
CREATE TABLE team_season_stats (
    team_id                 BIGINT NOT NULL REFERENCES team(id),
    league_id               BIGINT NOT NULL,
    season_year             INTEGER NOT NULL,
    form                    TEXT,             -- ex. "WWDLW"

    -- meciuri jucate
    played_home             INTEGER,
    played_away             INTEGER,
    played_total            INTEGER,
    wins_home               INTEGER,
    wins_away               INTEGER,
    wins_total              INTEGER,
    draws_home              INTEGER,
    draws_away              INTEGER,
    draws_total             INTEGER,
    loses_home              INTEGER,
    loses_away              INTEGER,
    loses_total             INTEGER,

    -- goluri marcate / primite (total + medie), split acasa/deplasare
    goals_for_home          INTEGER,
    goals_for_away          INTEGER,
    goals_for_total         INTEGER,
    goals_for_avg_home      NUMERIC(6,3),
    goals_for_avg_away      NUMERIC(6,3),
    goals_for_avg_total     NUMERIC(6,3),
    goals_against_home      INTEGER,
    goals_against_away      INTEGER,
    goals_against_total     INTEGER,
    goals_against_avg_home  NUMERIC(6,3),
    goals_against_avg_away  NUMERIC(6,3),
    goals_against_avg_total NUMERIC(6,3),

    -- clean sheets / failed to score
    clean_sheet_home        INTEGER,
    clean_sheet_away        INTEGER,
    clean_sheet_total       INTEGER,
    failed_to_score_home    INTEGER,
    failed_to_score_away    INTEGER,
    failed_to_score_total   INTEGER,

    -- cartonase agregate (bucketuri pe minute vin in fixture_*; aici totalul)
    yellow_cards_total      INTEGER,
    red_cards_total         INTEGER,

    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (team_id, league_id, season_year),
    FOREIGN KEY (league_id, season_year) REFERENCES season(league_id, year)
);

-- Statistici jucator per echipa+competitie+sezon (players[].statistics[]).
-- Baza pentru modelul de marcatori (la final).
CREATE TABLE player_season_stats (
    player_id        BIGINT NOT NULL REFERENCES player(id),
    team_id          BIGINT NOT NULL REFERENCES team(id),
    league_id        BIGINT NOT NULL,
    season_year      INTEGER NOT NULL,

    position         TEXT,
    appearances      INTEGER,
    lineups          INTEGER,
    minutes          INTEGER,
    rating           NUMERIC(5,3),
    captain          BOOLEAN,

    goals_total      INTEGER,
    goals_conceded   INTEGER,
    goals_assists    INTEGER,
    goals_saves      INTEGER,

    shots_total      INTEGER,
    shots_on         INTEGER,
    passes_total     INTEGER,
    passes_key       INTEGER,
    passes_accuracy  INTEGER,

    tackles_total    INTEGER,
    tackles_blocks   INTEGER,
    tackles_intercep INTEGER,
    duels_total      INTEGER,
    duels_won        INTEGER,
    dribbles_attempts INTEGER,
    dribbles_success INTEGER,

    fouls_drawn      INTEGER,
    fouls_committed  INTEGER,
    cards_yellow     INTEGER,
    cards_yellowred  INTEGER,
    cards_red        INTEGER,

    penalty_won      INTEGER,
    penalty_committed INTEGER,
    penalty_scored   INTEGER,
    penalty_missed   INTEGER,
    penalty_saved    INTEGER,

    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, team_id, league_id, season_year),
    FOREIGN KEY (league_id, season_year) REFERENCES season(league_id, year)
);

-- Clasament (endpoint /standings).
CREATE TABLE standing (
    league_id      BIGINT NOT NULL,
    season_year    INTEGER NOT NULL,
    team_id        BIGINT NOT NULL REFERENCES team(id),
    rank           INTEGER,
    group_name     TEXT,                      -- "group" din API (grupe cupe)
    points         INTEGER,
    goals_diff     INTEGER,
    form           TEXT,
    status         TEXT,                      -- "same"/"up"/"down"
    description    TEXT,                      -- ex. "Promotion - Champions League"

    played_all     INTEGER,
    win_all        INTEGER,
    draw_all       INTEGER,
    lose_all       INTEGER,
    goals_for_all  INTEGER,
    goals_against_all INTEGER,

    played_home    INTEGER,
    win_home       INTEGER,
    draw_home      INTEGER,
    lose_home      INTEGER,
    goals_for_home INTEGER,
    goals_against_home INTEGER,

    played_away    INTEGER,
    win_away       INTEGER,
    draw_away      INTEGER,
    lose_away      INTEGER,
    goals_for_away INTEGER,
    goals_against_away INTEGER,

    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (league_id, season_year, team_id),
    FOREIGN KEY (league_id, season_year) REFERENCES season(league_id, year)
);

-- =============================================================================
-- 3. MECIURI (fixtures) + detaliile lor
-- =============================================================================

-- Meci (endpoint /fixtures). Nucleul: din meciuri + statisticile lor calculam
-- forma ultimelor N meciuri, split acasa/deplasare.
CREATE TABLE fixture (
    id              BIGINT PRIMARY KEY,       -- id fixture din API
    referee         TEXT,
    timezone        TEXT,
    kickoff         TIMESTAMPTZ,              -- fixture.date
    league_id       BIGINT NOT NULL,
    season_year     INTEGER NOT NULL,
    round           TEXT,                     -- ex. "Regular Season - 12"
    venue_id        BIGINT REFERENCES venue(id),

    -- status (fixture.status)
    status_long     TEXT,                     -- ex. "Match Finished"
    status_short    TEXT,                     -- ex. "FT", "NS", "1H"
    status_elapsed  INTEGER,                  -- minutul curent (live)

    home_team_id    BIGINT NOT NULL REFERENCES team(id),
    away_team_id    BIGINT NOT NULL REFERENCES team(id),

    -- rezultat: goals + defalcare pe reprize
    goals_home      INTEGER,
    goals_away      INTEGER,
    score_ht_home   INTEGER,                  -- halftime
    score_ht_away   INTEGER,
    score_ft_home   INTEGER,                  -- fulltime
    score_ft_away   INTEGER,
    score_et_home   INTEGER,                  -- extratime
    score_et_away   INTEGER,
    score_pen_home  INTEGER,                  -- penalty shootout
    score_pen_away  INTEGER,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (league_id, season_year) REFERENCES season(league_id, year)
);

CREATE INDEX idx_fixture_league_season ON fixture(league_id, season_year);
CREATE INDEX idx_fixture_home_team ON fixture(home_team_id);
CREATE INDEX idx_fixture_away_team ON fixture(away_team_id);
CREATE INDEX idx_fixture_kickoff ON fixture(kickoff);
CREATE INDEX idx_fixture_status_short ON fixture(status_short);

-- Evenimente dintr-un meci (endpoint /fixtures/events): goluri, cartonase,
-- schimbari, VAR. Sursa exacta pentru "cine a marcat" si "minut cartonas".
-- player_id/assist_id sunt indexate dar fara FK dur (evenimentele pot sosi
-- inaintea profilului complet al jucatorului; data-collector face upsert minimal).
CREATE TABLE fixture_event (
    id            BIGSERIAL PRIMARY KEY,      -- surogat (API nu da id de eveniment)
    fixture_id    BIGINT NOT NULL REFERENCES fixture(id),
    team_id       BIGINT REFERENCES team(id),
    player_id     BIGINT,                     -- jucatorul implicat (marcator/faultat)
    assist_id     BIGINT,                     -- pasa de gol / jucator intrat la schimbare
    time_elapsed  INTEGER,                    -- minut
    time_extra    INTEGER,                    -- minut aditional (ex. 90+3 => 3)
    type          TEXT,                       -- "Goal" / "Card" / "subst" / "Var"
    detail        TEXT,                       -- "Normal Goal" / "Yellow Card" / ...
    comments      TEXT
);

CREATE INDEX idx_fixture_event_fixture ON fixture_event(fixture_id);
CREATE INDEX idx_fixture_event_player ON fixture_event(player_id);
CREATE INDEX idx_fixture_event_type ON fixture_event(type);

-- Statistici pe echipa intr-un meci (endpoint /fixtures/statistics).
-- AICI stau cornerele, faulturile, suturile, posesia — inima pietelor.
CREATE TABLE fixture_team_stats (
    fixture_id          BIGINT NOT NULL REFERENCES fixture(id),
    team_id             BIGINT NOT NULL REFERENCES team(id),

    shots_on_goal       INTEGER,
    shots_off_goal      INTEGER,
    shots_total         INTEGER,
    shots_blocked       INTEGER,
    shots_insidebox     INTEGER,
    shots_outsidebox    INTEGER,
    fouls               INTEGER,              -- faulturi (piata faulturi)
    corner_kicks        INTEGER,              -- cornere (piata cornere)
    offsides            INTEGER,
    ball_possession     NUMERIC(5,2),         -- procent (din "54%")
    yellow_cards        INTEGER,              -- (piata cartonase)
    red_cards           INTEGER,
    goalkeeper_saves    INTEGER,
    passes_total        INTEGER,
    passes_accurate     INTEGER,
    passes_percentage   NUMERIC(5,2),
    expected_goals      NUMERIC(6,3),         -- xG (poate lipsi pe planul free)

    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (fixture_id, team_id)
);

-- Formatia unei echipe intr-un meci (endpoint /fixtures/lineups).
CREATE TABLE fixture_lineup (
    fixture_id   BIGINT NOT NULL REFERENCES fixture(id),
    team_id      BIGINT NOT NULL REFERENCES team(id),
    formation    TEXT,                        -- ex. "4-3-3"
    coach_id     BIGINT REFERENCES coach(id),
    PRIMARY KEY (fixture_id, team_id)
);

-- Jucatorii din formatie (startXI + rezerve) — lineups[].startXI / substitutes.
CREATE TABLE fixture_lineup_player (
    fixture_id   BIGINT NOT NULL REFERENCES fixture(id),
    team_id      BIGINT NOT NULL REFERENCES team(id),
    player_id    BIGINT NOT NULL,             -- fara FK dur (vezi nota de la event)
    player_name  TEXT,
    number       INTEGER,                     -- numarul de pe tricou
    position     TEXT,                        -- "G"/"D"/"M"/"F"
    grid         TEXT,                        -- pozitie in teren, ex. "4:2"
    is_substitute BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (fixture_id, player_id),
    FOREIGN KEY (fixture_id, team_id) REFERENCES fixture_lineup(fixture_id, team_id)
);

-- Statistici individuale ale jucatorului intr-un meci
-- (endpoint /fixtures/players). Granularitate maxima pentru marcatori/carduri.
CREATE TABLE fixture_player_stats (
    fixture_id       BIGINT NOT NULL REFERENCES fixture(id),
    team_id          BIGINT NOT NULL REFERENCES team(id),
    player_id        BIGINT NOT NULL REFERENCES player(id),

    minutes          INTEGER,
    rating           NUMERIC(5,3),
    captain          BOOLEAN,
    substitute       BOOLEAN,
    position         TEXT,

    shots_total      INTEGER,
    shots_on         INTEGER,
    goals_total      INTEGER,
    goals_conceded   INTEGER,
    goals_assists    INTEGER,
    goals_saves      INTEGER,

    passes_total     INTEGER,
    passes_key       INTEGER,
    passes_accuracy  INTEGER,

    tackles_total    INTEGER,
    tackles_blocks   INTEGER,
    tackles_intercep INTEGER,
    duels_total      INTEGER,
    duels_won        INTEGER,
    dribbles_attempts INTEGER,
    dribbles_success INTEGER,

    fouls_drawn      INTEGER,
    fouls_committed  INTEGER,
    cards_yellow     INTEGER,
    cards_red        INTEGER,

    penalty_won      INTEGER,
    penalty_committed INTEGER,
    penalty_scored   INTEGER,
    penalty_missed   INTEGER,
    penalty_saved    INTEGER,

    PRIMARY KEY (fixture_id, player_id)
);

CREATE INDEX idx_fixture_player_stats_player ON fixture_player_stats(player_id);

-- =============================================================================
-- 4. ACCIDENTARI
-- =============================================================================

-- Accidentari / suspendari (endpoint /injuries).
CREATE TABLE injury (
    id           BIGSERIAL PRIMARY KEY,       -- surogat
    player_id    BIGINT NOT NULL REFERENCES player(id),
    team_id      BIGINT REFERENCES team(id),
    fixture_id   BIGINT REFERENCES fixture(id),
    league_id    BIGINT,
    season_year  INTEGER,
    type         TEXT,                         -- "Missing Fixture" / "Questionable"
    reason       TEXT,                         -- ex. "Knee Injury"
    reported_at  DATE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (player_id, fixture_id, type)
);

CREATE INDEX idx_injury_player ON injury(player_id);
CREATE INDEX idx_injury_team ON injury(team_id);
