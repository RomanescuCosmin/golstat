# Prompt / Plan de implementare — Pagina echipei + Căutare + Program + Live

> Acest fișier e un prompt de execuție pentru un agent: implementează workstream-urile de mai jos,
> în ordinea dată, incremental (un pas mic, curat și testat o dată). Respectă CLAUDE.md
> (termeni de domeniu în română, record-uri, comentarii minime, REST doar în modulul `api`).
> NU face commit/push — utilizatorul comite singur pe `main`.

## Context

Ținta: pagina echipei să arate exact ca designul `statistica-echipa.png`
(`Documents\poza app golstats`), cu o secțiune „Statistici" nouă cu progress bar-uri
procentuale pe goluri / cornere / faulturi / cartonașe; search bar funcțional pentru echipele
din ligile europene (sezon 2025-26); program de meciuri viitoare din toate competițiile
urmărite + amicale între cluburi (liga API-Football 667); match center live complet
(statistici, echipe de start, antrenor, arbitru, evenimente cu nume de jucători, schimbări, cartonașe).

Stare actuală (verificată):
- Pagina echipei există ~70% în frontend (`TeamPage` + `components/echipa/`), dar `sumar`,
  `statistici sezon`, `topJucatori`, `antrenor` citesc din tabele **nepopulate** — colectorul
  nu apelează `/teams/statistics`, `/players`, `/coachs`.
- Search: doar un input dezactivat în `TopNav`; niciun endpoint.
- Meciuri viitoare: doar per-ligă-per-zi (`/api/v1/meciuri?leagueId&data`); amicalele nu sunt în config.
- Live: evenimente + statistici se colectează **doar la meciuri terminate** — la un meci în
  desfășurare cronologia și statisticile ar fi goale. Lineups/arbitru există în DB
  (`fixture_lineup`, `fixture_lineup_player`, `fixture.referee`) dar nu sunt expuse în `MeciCentralDto`.
- Plan API-Football PRO (7500 req/zi) configurat în `application-local.yml` (gitignored)
  cu ligile europene pe sezonul 2025 + WC 1/2026.

Decizii deja luate cu utilizatorul:
- **Procentaje StatProcente**: relative la media ligii — `procent = 100 × mediaEchipei / (2 × mediaLigii)`,
  plafonat [0,100]; 50% = exact media ligii. Afișăm valoarea echipei și media ligii.
- **Program**: pagină nouă `/program` + card scurt „Urmează" pe pagina principală.
- **Search**: în baza locală (echipele deja colectate), nu în API-Football.

## Ordinea de implementare

1. **B — Căutare echipe** (mic, izolat, câștig imediat)
2. **C — Program meciuri viitoare + amicale**
3. **D2 + D3 — Match center: lineups/arbitru/stadion** (datele există deja în DB)
4. **A-backend — colectare `/teams/statistics`, `/players`, `/coachs`** (lăsăm să populeze DB)
5. **A-frontend — redesign pagina echipei + StatProcente**
6. **D1 + D4 — evenimente/statistici live în colector + reglaj polling**

---

## B. Căutare echipe

Backend:
- `TeamRepository`: query `search(q, Pageable)` — `lower(name) like %q%`, ordonare:
  cluburi înaintea naționalelor (`isNational asc`), potriviri prefix înaintea celor infix, apoi alfabetic.
- Nou `backend/api/src/main/java/ro/golstat/api/team/RezultatCautareDto.java`:
  `record(teamId, nume, logo, tara, nationala)`.
- `TeamController`: `GET /api/v1/echipe/cauta?q=` (min 2 caractere, limit 10) prin `TeamService.cauta(q)`.
  Path-ul literal `/cauta` are prioritate față de `/{teamId}` în Spring.

Frontend:
- `api/client.ts`: `cautaEchipe(q, signal)` (extinde `request()` cu `AbortSignal` opțional);
  `api/types.ts`: `RezultatCautare`.
- Nou `components/layout/CautareEchipe.tsx`: înlocuiește inputul dezactivat din `TopNav.tsx`;
  debounce 300ms, `AbortController` pentru cereri stale, dropdown (TeamLogo + nume + țară),
  navigare tastatură (↑/↓/Enter/Esc), select → `/echipa/:id`, empty state „Nicio echipă găsită".

Teste: `TeamServiceTest` — mapare + limită; smoke manual: „man" → Manchester City/United înaintea naționalelor.

## C. Program meciuri viitoare cross-competiții + amicale

Backend:
- `FixtureRepository.findUpcomingAll(ns, from, to)` — status NS, fereastră de zile,
  fără filtru de ligă, ordonat pe kickoff.
- Nou `backend/api/src/main/java/ro/golstat/api/live/ProgramDto.java`:
  `Zi(data, ligi[]) → Liga(leagueId, nume, tara, logo, meciuri[]) → Meci(fixtureId, kickoff, gazde, oaspeti)`.
- Nou `ProgramService.program(zile)` — grupare pe zi (UTC) apoi pe ligă
  (pattern `teamsById` din `MeciuriLiveController`, ligi prin `LeagueRepository.findAllById`).
  Endpoint `GET /api/v1/meciuri/urmatoare?zile=7` (1–14) în `MeciuriLiveController`.

Colector:
- `application-local.yml` (gitignored, editat local): adaugă `{ league-id: 667, season: 2026 }`
  (Friendlies Clubs — sezon an calendaristic; dacă log-ul din `CollectionService` arată 0 fixtures,
  încearcă și 2025 și păstrează ce returnează rânduri). Cost quota ≈ +50/zi.

Frontend:
- `lib/ligi.ts`: adaugă toate ligile urmărite (39,40,41,42,43,140,141,135,136,78,79,61,62,88,94,144,2,3)
  + 667 „Amicale cluburi".
- Nou `pages/ProgramPage.tsx` + rută `/program` în `App.tsx` + link „Program" în TopNav
  (înlocuiește un placeholder). Nou `components/program/SectiuneProgram.tsx`
  (header zi → card per competiție → rânduri cu oră, logo-uri, nume, link `/meci/:id`).
- Pe `MeciuriPage` (home): card „Urmează" cu primele meciuri din `getProgram(7)`, link spre `/program`.
- `urmatorulMeci` de pe pagina echipei e deja cross-competiție (`findNextForTeam` nu filtrează liga) —
  amicalele apar automat după colectare.

Teste: `ProgramServiceTest` (Mockito, ca `MatchCenterServiceTest`) — grupare zi/ligă, ordonare, fereastră goală.

## D2 + D3. Match center: lineups, arbitru, stadion

Backend (`MatchCenterService` / `MeciCentralDto`):
- Câmpuri noi: `arbitru` (`fixture.referee`), `stadion` (via `VenueRepository`, null-safe), și:
  - `Formatii(gazde, oaspeti)` → `EchipaFormatie(formatie, antrenor, titulari[], rezerve[])`
    → `JucatorDto(id, nume, numar, pozitie, grid)`.
- Construcție ca în `MatchPreviewService.echipeDeStart` (~liniile 133–172);
  antrenor prin `CoachRepository.findAllById`; blocul e `null` până există ambele lineup-uri
  (contractul existent: fiecare bloc degradează independent).

Frontend:
- `api/types.ts`: extinde `MeciCentral` cu `arbitru`, `stadion`, `formatii`.
- `HeaderScor.tsx`: rând „Arbitru: X · Stadion: Y" (doar când există).
- Nou `components/centru/FormatiiMeci.tsx`: refolosește `lineup/Teren`, `FormatieBadge`,
  `EchipaDeStart`, `ListaRezerve` (shape-ul jucătorului `{id, nume, numar, pozitie, grid}` se potrivește —
  verifică props la implementare); adaugă caption antrenor; fără `ListaIndisponibili` aici.
  Inserat între `StatisticiLive` și `CronologieMeci` în `MatchCenterPage.tsx`.

Teste: `MatchCenterServiceTest` — `formatii` cu ambele lineup-uri / null la lipsă; arbitru/stadion null-safe.

## A. Pagina echipei ca în design + StatProcente

### A1. Colector — surse noi de date
- `ApiFootballClient`: `ACCEPT_SINGLE_VALUE_AS_ARRAY` pe `JsonMapper` (răspunsul `/teams/statistics`
  e obiect unic, nu listă) + paginare: `Paging(current, total)` în `ApiFootballResponse` +
  `getPaged(path, params, itemType, ttl)` care iterează page=1..total (fiecare pagină = 1 slot quota,
  cache per pagină prin `cacheKey`-ul existent).
- Item-uri noi în `provider/apifootball/`: `TeamStatisticsItem` (cards vin pe bucket-uri de minute →
  însumate pentru totaluri), `PlayerItem` (profil + `statistics[]`), `CoachItem`
  (payload de carieră; antrenorul curent = intrarea cu `end == null` pe echipa cerută).
- `ApiFootballMapper`: `toTeamSeasonStats`, `toPlayer`, `toPlayerSeasonStats`, `toCoach`.
- `common`: `record PlayerSezonDto(PlayerDto profil, List<PlayerSeasonStatsDto> statistici)`
  (un singur call HTTP le aduce pe ambele — nu le despărți în două apeluri).
- `DataProvider` + `ApiFootballProvider` + `StubDataProvider`:
  `teamStatistics(leagueId, season, teamId)` TTL nou 20h; `players(teamId, season)` paginat TTL 7 zile;
  `coaches(teamId)` TTL 7 zile.
- `CollectionService.collectGoalsData`: după bucla de teams, per echipă publică pe topicele
  deja definite: `TEAM_SEASON_STATS` (cheie `teamId:leagueId:season`), `PLAYERS` (lot),
  `PLAYER_SEASON_STATS` (lot), `COACHES` (per antrenor curent). Fără scheduler nou —
  TTL-urile Redis fac ciclul orar auto-limitant; epuizarea de quotă degradează deja grațios.
- **Quota**: consum actual ≈ 3700/zi; A adaugă ≈ 650/zi amortizat (+ spike ~1600 la primul ciclu)
  → ≈ 4350/zi, în bugetul de 7500.

### A2. API — ingest
- `EntityMapper`: `toPlayer`, `toCoach`, `toTeamSeasonStats`, `toPlayerSeasonStats` (entitățile există).
- `IngestService`: `ingestPlayers`, `ingestCoach`, `ingestTeamSeasonStats`, `ingestPlayerSeasonStats` —
  cu `ensurePlayer` nou (pattern placeholder ca `ensureTeam`; FK dur pe `player_season_stats.player_id`,
  topicele sosesc asincron — nu te baza pe ordine).
- `DataIngestListeners`: 4 listeneri noi (loturile ca `List<>` prin `TypeReference`).

### A3. API — extindere `PaginaEchipaDto` (blocurile existente rămân neschimbate)
- `rezultateRecente: List<MeciForma>` — ultimele 10 terminale (extrage helper comun cu `forma`).
- `statProcente: List<StatProcent>` —
  `record StatProcent(String categorie, Double medieEchipa, Double medieLiga, Integer procent)`;
  categorii GOLURI/CORNERE/FAULTURI/CARTONASE (constante `GolstatConstants.Piata`):
  - medii echipă: goluri din `TeamSeasonStats.goalsForAvgTotal` (fallback: medie din fixtures);
    cornere/faulturi/cartonașe din `FixtureTeamStatsRepository.findForTeamSeason`;
  - medii ligă: goluri din `FixtureRepository.avgGoals` (`(avgGazde+avgOaspeti)/2`);
    restul din `FixtureTeamStatsRepository.avgCounts`;
  - `procent = round(100 × medieEchipa / (2 × medieLiga))`, plafonat [0,100];
    categorie omisă când lipsesc datele pe oricare parte.
- `sezoane: List<Integer>` — distinct din `team_season_stats` ∪ sezoanele fixture-urilor echipei
  (query nou: `select distinct f.seasonYear from Fixture f where homeTeamId=:t or awayTeamId=:t`), desc.
  Selectorul refolosește parametrii existenți `leagueId`/`sezon` ai controllerului — fără schimbare acolo.

### A4. Frontend — redesign `TeamPage` după `statistica-echipa.png`
Componente noi în `components/echipa/`:
- `TabsEchipa.tsx` — Prezentare | Rezultate | Meciuri | Statistici | Jucători | Transferuri
  (doar Prezentare activă, restul „În curând", pattern-ul din TopNav).
- `SelectorSezon.tsx` — dropdown pe `sezoane`, re-fetch `getEchipa(id, leagueId, sezon)`.
- `GraficForma.tsx` — line chart SVG hand-rolled (convenția: fără chart-lib): V=2/E=1/I=0
  pe `rezultateRecente` cronologic, polyline + puncte cu tokens `win`/`draw`/`accent`, etichete etape.
- `RezultateRecente.tsx` — listă: dată, acasă/deplasare, adversar+logo, scor, badge V/E/I,
  link `/meci/:id/centru`.
- `StatProcente.tsx` — bară per categorie: label, valoare „5.4 / meci", lățime = `procent`,
  marker la 50% + caption „media ligii: 4.9".
- `TeamPage.tsx` — layout ca în design: `AntetEchipa` → `TabsEchipa` + `SelectorSezon` →
  grilă (`GraficForma` + `UrmatorulMeci` / `StatBareSezon` + `StatProcente` / `RezultateRecente` +
  `ClasamentSnippet`) → `DistributieGoluri` → `TopJucatori`. `api/types.ts` actualizat.

Teste: mapper-e colector cu JSON-uri literale; `ApiFootballClientTest` coercion obiect-unic + paginare;
`CollectionServiceTest` publish-uri noi cu chei/loturi corecte; `IngestServiceTest` incl. `ensurePlayer`;
`TeamServiceTest` — statProcente cu numere cunoscute (6.0 vs 5.0 → 60%), plafon 100,
omitere la date lipsă, rezultateRecente fereastră 10, sezoane union+sort.

## D1 + D4. Live: evenimente + statistici în timp real

Colector:
- **Evenimente — gratis din `live=all`** (răspunsul le include inline; azi `FixtureItem` le aruncă):
  adaugă `List<EventItem> events` în `FixtureItem` (null la răspunsurile `/fixtures` normale).
  `DataProvider.liveFixtures()` returnează `record FixtureLiveDto(FixtureDto fixture, List<FixtureEventDto> evenimente)`
  (în common; maparea cu `toEvent(e, fixtureId)` existent). `LivePoller`: pe lângă publish `FIXTURES`,
  publică lotul pe `FIXTURE_EVENTS` cheie `fixtureId` (ingest-ul e deja idempotent delete+rewrite
  per fixture → fiecare poll reîmprospătează cronologia). **Zero quota extra.**
- **Statistici — throttled**: `DataProvider.liveFixtureStatistics(fixtureId)` cu TTL 0
  (bypass cache-ul de 24h). `LivePoller` ține `Map<Long, Instant> ultimulFetchStats`;
  la fiecare poll, pentru meciurile live urmărite unde `now - last >= stats-every-ms` →
  fetch + publish lot pe `FIXTURE_TEAM_STATS`; curăță intrările pentru meciuri care nu mai sunt live.
  Proprietăți noi în `LiveProperties`/yml: `golstat.live.stats-every-ms: 120000` +
  opțional `golstat.live.stats-leagues` (allowlist, gol = toate cele urmărite).
- **Quota**: ~55 req/meci la 120s; sâmbătă aglomerată ~40 meciuri live ≈ 2200/zi →
  total general ≈ 6600/zi, sub 7500. Reglaje dacă e nevoie: 180s / allowlist ligile mari (39/140/135/78/61/2/3).

Frontend:
- `useMatchCenter.ts`: polling live 25s → 15s (aliniat cu poll-ul colectorului;
  evenimentele vin la ~15s, statisticile la 120s server-side).

Flux (fără cod nou): WS push pe schimbare de scor funcționează deja
(`DataIngestListeners.onFixture` → `LiveBroadcaster` → `/topic/live/{fixtureId}`);
evenimentele/statisticile aterizează continuu în DB prin listenerii existenți; polling-ul REST le aduce în UI.
Smoke: `LiveSimController` (dev) pentru UI, apoi o seară cu meciuri reale pe profilul `local`.

Teste: `LivePollerTest` — evenimente inline publicate ca lot per fixture urmărit; cadență stats cu
`Clock` mutabil; ligi neurmărite sărite; quota exception înghițită.
`ApiFootballMapperTest` — `FixtureItem` cu events inline → `FixtureLiveDto`.

---

## Fișiere critice

- `backend/api/src/main/java/ro/golstat/api/team/TeamService.java` + `PaginaEchipaDto.java` + `TeamController.java` (A3, B)
- `backend/api/src/main/java/ro/golstat/api/matchcenter/MatchCenterService.java` + `MeciCentralDto.java` (D2)
- `backend/api/src/main/java/ro/golstat/api/live/MeciuriLiveController.java` + noi `ProgramService`/`ProgramDto` (C)
- `backend/api/src/main/java/ro/golstat/api/ingest/` — `IngestService`, `DataIngestListeners`, `EntityMapper` (A2)
- `backend/data-collector/src/main/java/ro/golstat/collector/collection/CollectionService.java` (A1, C)
- `backend/data-collector/src/main/java/ro/golstat/collector/live/LivePoller.java` + `provider/*` (D1)
- `backend/common/src/main/java/ro/golstat/common/dto/` — `PlayerSezonDto`, `FixtureLiveDto` noi
- `frontend/src/pages/TeamPage.tsx`, `ProgramPage.tsx` (nou), `MatchCenterPage.tsx`
- `frontend/src/components/echipa/*` (noi: `TabsEchipa`, `SelectorSezon`, `GraficForma`, `RezultateRecente`,
  `StatProcente`), `components/centru/FormatiiMeci.tsx` (nou), `components/layout/CautareEchipe.tsx` (nou),
  `components/program/SectiuneProgram.tsx` (nou)
- `frontend/src/api/client.ts`, `types.ts`, `lib/ligi.ts`, `App.tsx`

## Verificare

1. `gradlew.bat build` din rădăcină — toate testele JUnit trec.
2. Infra locală (`docker compose up -d`) + api + collector profil `local` + `npm run dev` în frontend.
   Atenție: api scrie în Postgres LOCAL (127.0.0.1:5432), nu în containerul Docker.
3. Smoke:
   - search „man" în TopNav → dropdown → pagina echipei;
   - `/program` arată UCL 07.07.2026 + amicale, grupate pe zi/competiție;
   - pagina echipei are toate blocurile populate după un ciclu de colectare
     (antrenor, top jucători, statistici sezon, StatProcente cu procente corecte);
   - `/meci/:id/centru` arată arbitru, stadion, formații pe teren;
   - la un meci live real (sau `LiveSimController` pentru UI), cronologia și statisticile se actualizează.
4. NU face commit — utilizatorul comite pe `main`.
