# PROMPT: Pagina de Previzualizare Meci — Predicții & Statistici Avansate

## Rol

Ești un **expert senior în Java (Spring Boot) și React**, cu experiență vastă în **algoritmi de statistică sportivă și aplicații de pariuri sportive**. Lucrezi împreună cu agenți performanți, specializați în modelare probabilistică (Poisson, Negative Binomial, Dixon-Coles) și în design de interfețe pentru platforme de statistici sportive. Execuția trebuie să fie **perfectă**: cod curat, testat, algoritmi corecți matematic și un UI unic.

## Context

Lucrăm la pagina de **Previzualizare Meci** (`http://localhost:5174/meci/1576756`) din aplicația golstat (Spring Boot + React + PostgreSQL). Pagina trebuie să afișeze predicții și statistici pre-meci, în același stil vizual ca statisticile existente pentru o echipă.

## ⚠️ IMPORTANT: Lucru deja început

**Există deja cod început pe această pagină** — NU porni de la zero. Înainte de orice implementare:

1. **Analizează ce există deja**: pagina de preview funcționează parțial, iar în working tree sunt fișiere modificate necomise legate de acest feature (printre care `MatchPreviewService`, `EchipaDeStartDto`, `FixtureLineupRepository`, `FixtureLineupPlayerRepository` + testele aferente `MatchPreviewServiceTest`, `MatchPreviewControllerTest`).
2. **Evaluează critic codul existent** (backend + frontend-ul paginii de meci): ce e corect păstrezi și extinzi; ce nu corespunde cerințelor de mai jos **modifici sau refactorizezi** — nu duplica logică și nu lăsa cod vechi inconsistent lângă cel nou.
3. Integrează cerințele noi în structura existentă, menținând testele verzi și adăugând teste pentru tot ce e nou.

## Cerințe funcționale

### 1. Rezultate (formă recentă)

- Ultimele **7 meciuri ACASĂ** pentru echipa gazdă.
- Ultimele **7 meciuri în DEPLASARE** pentru echipa oaspete.
- Suplimentar, pentru **ambele echipe**: ultimele 7 meciuri combinate (acasă + deplasare).

### 2. Statistici (secțiunea centrală — cea mai importantă analiză tehnică)

Pentru fiecare piață de mai jos, analiza se face pe **ultimele 7 meciuri**, separat: echipa gazdă ACASĂ și echipa oaspete în DEPLASARE, dar afișând și statistica generală a fiecărei echipe (acasă respectiv deplasare):

- **Cartonașe**: numărul de cartonașe în ultimele 7 meciuri pentru fiecare echipă → calculează probabilitatea pe liniile relevante; menționează explicit statistica fiecărei echipe acasă/deplasare.
- **Cornere**: același principiu + analizează în câte din ultimele 7 meciuri (per echipă, acasă respectiv deplasare) s-au depășit liniile **7.5 / 8.5 / 9.5 / 10.5 cornere** → probabilitate per linie.
- **Goluri**: liniile **1.5 / 2.5 / 3.5** — același principiu ca la cornere. **GG (ambele marchează)** se calculează în funcție de golurile marcate ȘI primite de fiecare echipă.
- **Faulturi**: același principiu ca la celelalte secțiuni.
- **Pauză sau Final Egal**: probabilitatea de egal la pauză și de egal la final, calculată pe ultimele 7 meciuri.
- **Goluri în prima repriză / Goluri în a doua repriză**: frecvență și probabilitate pe ultimele 7 meciuri.

**Legendă obligatorie** lângă fiecare secțiune: explică pe ce se bazează statistica, în format explicit de tip *„a marcat în 5/7 meciuri"*, *„peste 9.5 cornere în 4/7 meciuri acasă"* etc.

**Rigoare algoritmică**: fă o analiză atentă — cercetează pe internet metodele consacrate (Poisson pentru cornere/goluri, Dixon-Coles pentru scoruri mici, Negative Binomial pentru faulturi/cartonașe, ponderare pe recență, shrinkage spre media ligii pentru eșantioane mici) și construiește algoritmi cât mai exacți, unici și potriviți fiecărei piețe. Nu media simplă — model probabilistic real.

### 3. Echipe probabile (pre-meci)

- Dacă meciul **nu a început**, afișează **echipele probabile** preluate din **API-Football** (endpoint lineups/predicted).
- Momentan nu mai avem requesturi disponibile — **scrie complet partea de cod** (client, cache, fallback), urmând să fie validată când se reface cota.

### 4. Echipele de start — fețele jucătorilor

- La echipele de start afișează **fotografia (fața) fiecărui jucător**.
- Dacă fotografia nu există (frecvent la ligi inferioare), **fallback la afișarea actuală** (cum apare acum).

## Cerințe de design

- Design **cât mai unic**: progress bar-uri custom, originale (nu componente generice), consistente cu stilul actual al paginilor de echipă.
- Fiecare piață cu vizualizare proprie a probabilității + legenda aferentă.

## Constrângeri tehnice

- Backend: Spring Boot, JPA, PostgreSQL — calculele statistice în module testabile (teste unitare cu exemple numerice cunoscute).
- Frontend: React (Vite, port 5174).
- API-Football: abstractizat prin interfața `DataProvider`, cache agresiv (cota pe minut e limitată — backoff la 429).
- Incremental: pași mici, curați și testați; fără scope redus din proprie inițiativă.
