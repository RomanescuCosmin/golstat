# Context pentru agenți — golstat

> Sursa unică de context. Citește ACEST fișier înainte de orice lucru pe golstat;
> completează/actualizează secțiunile când realitatea se schimbă (mai ales snapshot-ul de date).
> Ultima actualizare: 2026-07-20.

## 1. Ce este golstat

Aplicație de statistici și predicții fotbal, inspirată de Flashscore/Sofascore dar cu accent pe
statistici: calculează „șansa reală în procente" pentru piețe (goluri peste/sub, GG/NG, cornere,
faulturi, cartonașe, egal la pauză/final) din forma ultimelor meciuri, separată pe acasă/deplasare.
Cota afișată = `1/probabilitate` (stil Flashscore), plus cota informativă a furnizorului.

## 2. Arhitectura reală

Gradle multi-modul (rădăcina = `golstat/`), Java 21, React în `frontend/`.

```
API-Football (api-sports.io, plan PRO 7500 req/zi)
   │  ApiFootballClient: cache-first Redis + QuotaGuard (cota zilnică) + backoff limita/minut
   ▼
data-collector (Spring Boot) ── publică JSON pe Kafka (topicuri golstat.*)
   ▼
api (Spring Boot) ── DataIngestListeners → IngestService (upsert idempotent, placeholder-e pt FK)
   ▼
Postgres 18 LOCAL (baza `golstat`) ── REST /api/v1/* ── frontend React
```

- `common` — DTO-uri partajate + `GolstatConstants` (topicuri, endpoint-uri, statusuri meci).
- `stats-engine` — matematică pură (Poisson, Dixon-Coles, Negative Binomial, shrinkage, ferestre
  de formă), fără Spring; API-ul îl apelează prin mapări entitate→sample.
- Piețele pe zile (`/api/v1/piete/zile`) și pagina de meci folosesc ACELAȘI motor — există un
  test de invariant că procentele sunt identice (`PieteZileServiceTest.aceleasiProcenteCaPaginaDeMeci`).

## 3. Producția (cum rulează efectiv)

- **Windows Task Scheduler**, task `golstat-colectare`, la 3 ore, cu **wake-timer din sleep S3**
  (instalat de `scripts/instaleaza-task.ps1`, rulat o dată ca Admin).
- Task-ul rulează `scripts/colectare-oneshot.ps1 -Adoarme`: pornește Docker (doar kafka+redis),
  build incremental al jar-elor, pornește API-ul, rulează colectorul cu profil **`oneshot`**
  (un ciclu și iese), așteaptă drenarea Kafka (lag 0 pe grupul `golstat-api`), loghează cota
  consumată, apoi readoarme laptopul dacă e idle.
- Profilul activ de producție = `backend/data-collector/src/main/resources/application-oneshot.yml`:
  26 de ligi zilnice pe `season: 2026`, backfill cu cursor Redis, `zile-in-urma: 3`,
  `zile-inainte: 30`, rezervă zilnică 1500 din cota 7500, **live dezactivat**.
- Loguri: `scripts/logs/colectare-YYYY-MM-DD.log` (jurnalul ciclului) și
  `colectare-colector-YYYY-MM-DD.log` (ieșirea completă a colectorului, cu stack trace-uri);
  ambele intră în rotația de 14 zile.
- **Scriptul NU reconstruiește jar-ele dacă ele există** — build-ul se face doar la bootstrap, cu
  `--no-daemon`, fiindcă lock-ul global Gradle se ciocnește de daemonul IntelliJ (măsurat: blocaj
  de 19 minute, adică un ciclu programat irosit integral). După orice modificare de cod trebuie
  rulat manual `gradlew.bat :api:bootJar :data-collector:bootJar`; dacă uiți, scriptul scrie
  ATENȚIE în log că jar-ul e mai vechi decât sursele, dar tot rulează codul vechi.
- Task-ul are DOUĂ declanșatoare (orar la 3h + la deblocarea sesiunii, cu 1 min întârziere), deci
  două cicluri chiar se pot suprapune. Colectorul leagă portul 8082, așa că al doilea murea la
  bind cu cod 1 și părea o colectare eșuată. Scriptul are acum o gardă pe fișierul PID: dacă un
  ciclu rulează, al doilea iese curat cu „Sărit".
- **Postgres-ul real e cel LOCAL (serviciu Windows, Postgres 18, port 5432, DB `golstat`,
  user `postgres`)**. Containerul Docker `golstat-postgres` trebuie să stea OPRIT — ar umbri
  portul 5432 și datele ar ateriza tăcut într-o bază goală; scriptul îl oprește singur.

## 4. Convenții de date

- **`season: 2026` = sezonul 2026/27** (anul de start, convenția API-Football) și include vara
  2026 (Cupa Mondială, amicale, preliminarii). Excepție: ligile pe an calendaristic
  (113 Allsvenskan, 103 Eliteserien) unde 2026 = anul 2026.
- Nu există rezolvare automată a „sezonului curent" — sezoanele sunt literale în YAML. La
  schimbarea de sezon (august 2027) trebuie actualizate toate profilele + adăugate ținte de
  backfill pentru partea deja jucată (regula e documentată în comentariile din
  `application-oneshot.yml`).
- O ligă adăugată la mijloc de sezon are nevoie și de o țintă cu fereastră absolută
  (`from`/`to`, CU ghilimele în YAML, altfel binding-ul crapă), altfel meciurile deja jucate nu
  se aduc niciodată. Cursorul de backfill: cheie Redis `golstat:backfill:<liga>:<sezon>` = DONE.
- Doar meciurile cu status TERMINAL (`FT`, `AET`, `PEN`) primesc detalii per meci
  (evenimente, statistici, formații) și doar ele intră în ferestrele de formă.
- Amicalele (liga 667) se colectează `doar-fixtures: true` — scor și program, FĂRĂ statistici
  per meci și fără îmbogățire de echipe (mii de echipe ar arde cota). E intenționat.
- Coverage-ul declarat de furnizor (`statistics_fixtures`) MINTE pe sezoane noi — colectorul
  sondează până la 3 meciuri terminale reale înainte să renunțe la statisticile unei ligi.
- Răspunsurile GOALE la detaliile unui meci terminat (statistici/evenimente/formații nepublicate
  încă de furnizor) se cache-uiesc SCURT (`ttl-detalii-goale`, 15 min) — nu 24h ca datele pline.
  Fix din 2026-07-20; înainte, golul cache-uit 24h + fereastra de 1 zi pierdeau definitiv
  statisticile meciurilor abia terminate. De aceea acoperirea istorică are găuri (vezi §5).

## 5. Starea datelor (snapshot 2026-07-20, înainte de fix)

- 12.498 fixtures (2024-08 → 2027-05); 3.462 pe season 2025, 9.026 pe season 2026
  (din care 7.078 amicale 667).
- **Acoperire `fixture_team_stats` pe meciuri terminate: ~50% pe 2025, ~5% pe 2026** (procentul
  pe 2026 e diluat de amicale, care nu vor avea niciodată statistici). Cauza istorică = bug-ul
  de cache descris în §4; decizie utilizator: NU se recuperează retroactiv (nu ardem quota pe
  trecut), doar prevenim de-acum încolo. Meciurile noi ar trebui să tindă spre ~100%.
- `fixture_player_stats` e GOL — nicio țintă nu are `statistici-jucatori: true` (cost ~1 req/meci).
- Anomalii minore cunoscute: 4 fixtures cu `season_year=0`, 4 cu status gol, 603 CANC,
  6 reziduale pe 2024.
- Arbitrul e doar text liber în `fixture.referee` (nu există entitate de arbitru).

## 6. Decizii deliberate / probleme cunoscute lăsate așa

Confirmate cu utilizatorul pe 2026-07-20 — NU le „repara" din proprie inițiativă:

1. **Fără backfill retroactiv** al statisticilor lipsă (quota se păstrează pentru prezent).
2. **Ferestrele de formă amestecă competiții și sezoane** — „ultimele 7 meciuri" ale echipei
   includ cupe, amicale, sezonul trecut (`FixtureRepository.findRecentForTeam` nu filtrează pe
   ligă/sezon). Utilizatorul a ales explicit să rămână așa.
3. **HT lipsă e tratat ca 0-0** în `MatchSampleMapper` — piața „egal pauză" numără meciurile
  fără scor de pauză ca egaluri. Cunoscut, neschimbat.
4. **`ScheduleFilter` (filtrul „jumătatea de jos a clasamentului") e calculat dar necablat** —
   rangul adversarului se populează dar nu influențează niciun procent afișat.
5. **LIVE e dezactivat în producție** — scoruri/statusuri se actualizează doar la ciclul de 3h.
   Polling-ul live (15s) există doar pe profilul `local` (proces pornit manual, continuu).
6. Statusurile speciale (ABD, AWD, WO, SUSP, INT, BT) nu sunt în `GolstatConstants.FixtureStatus`
   și nu primesc tratament dedicat.

**Două surse diferite, două lipsuri diferite** (important la „fără date"): `fixture_team_stats`
(cornere, faulturi, cartonașe, șuturi, posesie) și `fixture_event` (goluri, cartonașe, schimbări,
VAR) vin din endpoint-uri separate ale furnizorului și pot lipsi independent. La ligile cu
acoperire parțială (tipic Liga I 2026, care declară `statistics_fixtures=false`) se întâmplă des
să avem evenimentele dar nu statisticile. De aceea **cartonașele se numără din cronologie când
statisticile lipsesc** (`MatchPreviewService.cartonase` + `FixtureEventRepository.countCards`);
verificat pe 6920 de meciuri cu ambele surse: coincide în 6903 (99,75%). Cornerele și faulturile
NU se pot recupera așa — nu există ca evenimente; când furnizorul nu le are, chiar nu le știm.

Regulă de afișare (fix 2026-07-21): pe pagina de meci, procentul modelat al unei piețe NU se
afișează când ferestrele de locație ale ambelor echipe sunt goale pe piața respectivă — fără
meciuri cu cornere/cartonașe/faulturi măsurate, modelul ar da doar media ligii (procent
fals-sigur, ex. „86%"). Se afișează „—", fără verdict ✓/✗ (`RandLinie`/`RandGg` în
`SectiuneStatistici.tsx`). Pagina `/piete` filtrează deja la sursă: piețele cu eșantion 0 nu se
trimit (`PieteZileService.adaugaLinii`).

## 7. Cum rulezi și verifici

```powershell
# Build + toate testele (din rădăcina golstat/)
.\gradlew.bat build

# Un ciclu de colectare manual, identic cu producția (necesită API_FOOTBALL_KEY în env)
powershell -File scripts\colectare-oneshot.ps1        # fără -Adoarme ca să nu-ți adoarmă PC-ul

# Frontend
cd frontend; npm test; npm run dev
```

Verificarea acoperirii statisticilor (psql local, DB `golstat`):

```sql
SELECT f.league_id, count(*) AS terminate,
       count(*) FILTER (WHERE EXISTS (
           SELECT 1 FROM fixture_team_stats s WHERE s.fixture_id = f.id)) AS cu_stats
FROM fixture f
WHERE f.status_short IN ('FT','AET','PEN')
  AND f.kickoff > now() - interval '3 days'
  AND f.league_id <> 667
GROUP BY 1 ORDER BY 1;
```

Verificarea cache-ului de goluri (Redis din docker):
`docker exec golstat-redis redis-cli TTL "<cheie golstat:af:cache:/fixtures/statistics...>"` —
pe un meci abia terminat fără statistici, TTL ≤ 900s, nu ~86400.

Browser-ul automatizat NU ajunge la localhost — verifică demo-urile prin curl/PowerShell.

## 8. Ce să NU faci

- NU porni containerul `golstat-postgres` (`docker compose up -d` FĂRĂ argumente îl pornește!) —
  folosește `docker compose up -d kafka redis`.
- NU lărgi lista de ligi / nu activa `statistici-jucatori` fără calcul de cotă (bugetele sunt
  în comentariile din `application-oneshot.yml` și `docs/plan-implementare.md`).
- NU șterge Redis fără să înțelegi costul: cursoarele de backfill marcate DONE previn
  re-colectări de ~9600 requesturi.
- NU adăuga ținte de backfill pentru ligile deja colectate pe 2025 (lista în
  `OneshotProfileConfigTest.alreadyCollectedLeaguesAreNotScheduledAgain`).
- NU rula `git commit/push` din scripturi automate; workflow-ul e cu branch-uri GS-NN + PR.
