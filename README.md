# golstat

Aplicatie de statistici si predictii fotbal — calculeaza "sansa reala in procente"
pentru cornere, goluri, faulturi, cartonase si marcatori, din forma ultimelor meciuri,
separata pe acasa / deplasare.

> Status: **schelet**. Structura e pusa la punct; modulele se construiesc separat, pas cu pas.

## Stack

- Backend: Spring Boot (Java 21), Gradle multi-modul
- Frontend: React (de scafoldat)
- Mesagerie: Apache Kafka + Schema Registry
- Date: PostgreSQL / TimescaleDB + Redis
- Sursa: API-Football (api-sports.io)

## Structura

```
backend/      Gradle multi-modul: common, stats-engine, data-collector, api
frontend/     React (placeholder)
infra/sql/    migrari SQL manuale (gol)
docker-compose.yml
```

## Pornire infra locala

```bash
docker compose up -d
```

## Build backend

Wrapper-ul Gradle e in radacina proiectului; se buildeste tot backend-ul (toate modulele)
direct din `golstat/`, fara sa mai deschizi doar `backend/`.

```bash
./gradlew build          # Windows: gradlew.bat build
```

## Pasi urmatori

Construim incremental, fiecare bucata curata si testata:

1. `stats-engine` — modelele matematice (Poisson, Dixon-Coles, Negative Binomial, forma + shrinkage)
2. Schema DB in `infra/sql`
3. `data-collector` — client API-Football + Kafka
4. `api` — REST + WebSocket
5. `frontend` — React
6. Model marcatori (la final)
