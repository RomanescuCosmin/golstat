# CLAUDE.md — golstat (Football Stats Engine)

## Project Overview

Aplicatie de statistici si predictii fotbal: calculeaza "sansa reala in procente"
pentru piete (cornere, goluri, faulturi, cartonase, marcatori) din forma ultimelor
meciuri, separata pe acasa / deplasare.

- **Group / package**: `ro.golstat`
- **Stack**: Spring Boot (backend, Java) + React (frontend) + Kafka + PostgreSQL/TimescaleDB + Redis
- **Build**: Gradle multi-modul (monorepo)
- **Status**: SCHELET. Niciun modul nu are inca implementare; le construim separat, pas cu pas.

---

## Repository Layout

```
golstat/                   → radacina Gradle (wrapper + settings + build.gradle)
  settings.gradle          → declara modulele (mapate in backend/)
  build.gradle             → config comuna (toolchain, junit, repositories)
  gradlew / gradlew.bat    → Gradle wrapper (build din radacina)
  backend/                 → Gradle multi-modul (Java 21)
    common/                → modele/DTO partajate (gol)
    stats-engine/          → algoritmii de predictie, Java pur, fara framework (gol)
    data-collector/        → colectare date (API-Football) + publish Kafka (gol)
    api/                   → REST + WebSocket; consuma stats-engine (gol)
  frontend/                → React (de scafoldat separat)
  infra/
    sql/                   → migrari SQL manuale (gol; DB se construieste separat)
  docker-compose.yml       → Postgres/TimescaleDB + Redis + Kafka + Schema Registry
```

---

## Build & Run

```bash
# Infra locala (Kafka, Postgres, Redis, Schema Registry)
docker compose up -d

# Build backend — din RADACINA proiectului (golstat/), nu din backend/
./gradlew build          # Windows: gradlew.bat build
```

> Radacina Gradle e `golstat/`: wrapper-ul, `settings.gradle` si `build.gradle` stau aici,
> iar modulele sunt mapate in `backend/` prin `settings.gradle`. Deschide direct `golstat/`
> in IntelliJ — importa tot proiectul multi-modul dintr-o data.

---

## Module Responsibilities

| Modul | Rol | Dependinte planificate |
|-------|-----|------------------------|
| `common` | Record-uri / DTO partajate intre module | Jackson |
| `stats-engine` | Modele matematice (Poisson, Dixon-Coles, Negative Binomial, forma, shrinkage). **Java pur, fara Spring, usor de testat.** | doar JUnit |
| `data-collector` | Client API-Football, cache Redis, scheduler, producers Kafka | Spring Boot, Kafka, Redis |
| `api` | REST + WebSocket, JPA, expune predictiile | Spring Boot Web, JPA, Postgres |

Adaugam dependintele si plugin-ul Spring Boot pe fiecare modul ATUNCI cand il construim,
nu inainte. Tinem build-ul curat.

---

## Algorithm (planificat, neimplementat inca)

`λ = MediaLiga × FortaAtac × SlabiciuneAparare`, apoi distributie → procente pe linii.

- **Forma**: ultimele N meciuri, split acasa/deplasare, ponderare pe recenta, shrinkage spre media ligii (esantion mic)
- **Cornere / goluri**: Poisson (goluri: corectie Dixon-Coles pentru scoruri mici)
- **Faulturi / cartonase**: Negative Binomial (supra-dispersie); factor arbitru la cartonase
- **Marcatori**: model ierarhic (LA FINAL)

---

## Code Conventions

- **Limba**: termeni de domeniu si comentariile existente in romana
- **Comentarii**: cod cat mai putin comentat. Comentam DOAR ce chiar necesita explicatie
  (o decizie ne-evidenta, un workaround, o formula). Fara comentarii care repeta codul.
- **stats-engine**: Java pur, deterministic, fara dependinte de framework; fiecare model are teste unitare cu exemple numerice cunoscute
- **DTO/record**: prefera `record` pentru modele imutabile
- **Fara REST in alte module decat `api`**
- **Incremental**: un pas mic, curat si testat o data; confirmam inainte de schimbari mari

---

## Data Source

API-Football (api-sports.io). Plan free: 100 requests/zi, toate endpoint-urile.
Abstractizeaza sursa printr-o interfata `DataProvider` din prima zi (schimb usor de furnizor).
Cache agresiv in Redis + polling inteligent pentru a economisi cota.
