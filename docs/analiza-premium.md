# Analiză „statistici premium” — cum prezintă piața cornerele, cartonașele și golurile

Data cercetării: 13.11.2026. Metodă: pagini publice + rezultate de căutare.

> **Limite ale cercetării.** SoccerSTATS, Forebet, TotalCorner și AdamChoi au răspuns 403 sau
> cu pagină goală. Ce e marcat **[neverificat]** nu a putut fi confirmat din sursă.
> Bine acoperite: FootyStats, Corner-stats.com, StatsHub (pagina de arbitri).
> Parțial: SoccerSTATS, TotalCorner, WhoScored, Forebet, Flashscore. Aproape deloc: SofaScore,
> FotMob, AdamChoi. Discuții Reddit relevante nu am găsit.

## Ce fac bine aplicațiile de referință

**FootyStats** (verificat)
- Scară de linii pe echipă într-un singur tabel: `7.5 | 8.5 | 9.5 | 10.5 | 11.5 | 12.5 | AVG`; utilizatorul vede cum scade procentul de la o linie la alta.
- Tab-uri „Total Corners / Corners For / Corners Against” și tabele pe reprize (la nivel de ligă) cu `+4.5 | +5.5 | +6.5`.
- Distribuție „Frequent Total Corners” (`Total Corners | % | # of times`) — arată forma distribuției, nu doar un procent.
- Prag minim de eșantion declarat: „Team must have played at least 7 matches to be eligible on this list.”
- Cota casei lângă procent („9.5+ Odds”), dată de actualizare vizibilă și pagină de arbitri cu cartonașe/meci, medii acasă/deplasare și „2.5+ cards %”.

**StatsHub** (verificat: `/features`, `/referees`)
- Pagina de arbitri e cel mai bun model găsit: linii **3.5 / 4.5 / 5.5 cartonașe**, filtru „Minimum games (10+, 25+, 50+, 100+)”, „This season only”, „Games: All / Last 20”, „Competition: All / Next game's only”, prag de rată de reușită (50%+ … 80%+), clasificare „Lenient / Average / Strict” și „Beats the price (Over/Under)”.
- „Highlight winning stats”: rata de reușită la linia efectivă a casei (Bet365, Kambi).
- Totul gratuit. Rate pe ultimele 5/10/15 la jucători apar doar într-o documentație-oglindă neoficială — **[neverificat]**.

**Corner-stats.com** (verificat)
- Propoziții de tendință generate automat, cu numărător, numitor, context și medie (vezi secțiunea 4).
- Tab-uri „1 half / 2 half” pe goluri, cornere, cartonașe; intervale de 15 minute sau interval de minute ales.
- Filtre: acasă/deplasare, „adversari similari”, etapă a turneului, dată, arbitru.

**SoccerSTATS** (din rezultate de căutare)
- Comutator de domeniu: „home games only for home team and away games only for visiting team” sau „last 8 matches”.
- Procente pe scara de goluri (0.5 / 1.5 / 2.5 …) inclusiv la pauză, separat acasă/deplasare; intervale de 15 minute; puncte pe meci. Cornere: **[neverificat]**.

**Flashscore** (articol oficial)
- Statisticile de meci sunt grupate „TOP Stats / Shots / Attack / Passes / Defence / Goalkeeping”, iar TOP Stats sunt „your go-to key stats” (inclusiv cornere și cartonașe). Fiecare metrică nouă (xGOT, xA) are o definiție în limbaj simplu.

**SofaScore**: comparație pe sezon întreg, nu doar H2H; interfață descrisă ca „exceptionally clean and fast”. Serii și tab-uri pe reprize: **[neverificat]**.

**TotalCorner** (fragmente de căutare): un singur rând pe meci cu handicap, cornere final și la pauză, linie de cornere, peste/sub, goluri, atacuri periculoase.

**WhoScored**: etichete calculate de stil, puncte tari și puncte slabe, fiecare susținută de o cifră; „Match Facts” cu indicii pentru pariuri. Formulări exacte **[neverificat]**.

**Forebet**: probabilități din model pentru rezultat și goluri; predicții de cornere și cartonașe există, structura e **[neverificată]**.

**AdamChoi**: un director web îl descrie ca acoperind „BTTS | Corners | Cards | Booking Points | Over 2.5 Goals”; restul **[neverificat]**.

**FotMob**: date de cornere sau cartonașe pe reprize **[neverificat]**.

## Ce fac prost

- **FootyStats — date vechi și inconsecvente** *(observat pe pagină)*: pagina de arbitri, marcată „Last Updated Nov 12, 2026”, are în tabel arbitri retrași (Mike Dean). Media lui apare 4,04 în intro și 3,96 în tabel.
- **FootyStats — încredere în premium** *(Trustpilot, 4,2/5 din 308 recenzii)*: „Their claims of large daily profits did not match my actual results”; cote „totally out of sync with the real world”; cereri de rambursare fără răspuns. Laude: „user friendly and no ads”, „covers minor leagues”.
- **FootyStats — paywall pe ligă și aglomerare** *(observat)*: rânduri „Premium League” cu celule goale; pagina de cornere are peste 10 tabele și nicio ierarhie. Premium: 19,99 £/lună (afișat ca reducere de la 26,99 £ pentru România).
- **FootyStats — split anunțat, dar neafișat** *(observat)*: textul promite procente acasă/deplasare pe care tabelul principal nu le arată.
- **Corner-stats — preț și complexitate** *(pagina de prețuri)*: 35 $/lună (230 $/an); pachetele combină țări, indicatori și unelte și sunt greu de înțeles.
- **Forebet — fără validare** *(recenzie ValueTheMarkets)*: nu publică backtest-uri sau calibrare și nu explică variabilele.
- **Rata de reușită fără cotă** *(ghid Betshoot)*: o rată mare nu înseamnă profit.
- **Medii fără distribuție** *(rezultat de căutare despre arbitri)*: o medie de 4,5 cartonașe nu spune cât de des se trece de 4,5.
- **Seriile ca semnal** *(articole de statistică)*: seriile apar inevitabil și în date aleatoare.
- **FotMob / Flashscore** *(recenzii App Store, agregatoare)*: reclame intruzive, funcții gratuite restrânse în timp.

## Linii și piețe folosite de piață

| Piață | Meci | Pe echipă | Pe repriză |
|---|---|---|---|
| Cornere | 7.5–12.5 (principală 9.5; FootyStats până la 15.5) | medii obținute/cedate; linii uzuale 3.5 / 4.5 / 5.5 **[neverificat la sursă]** | 4.5 / 5.5 / 6.5 la nivel de ligă (FootyStats); „over 4.5 first-half corners” (StatsHub). **Pe echipă pe repriză: negăsit nicăieri** |
| Goluri | 0.5–4.5 | marcat / primit, fără gol primit; 0.5 / 1.5 **[neverificat]** | 0.5+ și 1.5+ pe repriza 1/2; „a marcat în ambele reprize” |
| Cartonașe (arbitru) | 2.5+ (FootyStats); 3.5 / 4.5 / 5.5 (StatsHub) | „cel puțin 1 cartonaș în repriza 1” (Corner-stats); 1.5 / 2.5 **[neverificat]** | cartonașe pe reprize (Corner-stats) |
| Faulturi, ofsaiduri | totaluri, diferențe între echipe (Corner-stats) | „a câștigat la faulturi” | — |
| Derivate de cornere | handicap ±2.5 / −3.5 / −4.5, cursă până la 3/5/7, primul corner, par/impar (StatsHub) | — | — |

## Cum formulează textele

Corner-stats (citate exacte):
- „Bradford won on corners in 8 out of 10 matches at home this season (+3.30 avg. diff.)”
- „In 8 out of 8 matches of Union Berlin in Bundesliga this season, the team got at least 1 card in the 1st half (1.75 on avg.)”
- „Germany won on fouls in 7 out of 10 last matches with similar opponents (+2.70 avg. diff.)”

FootyStats: „Team must have played at least 7 matches to be eligible on this list.” / „remember that there are never guarantees in predictions!”
StatsHub: „Card and penalty records for every referee, and who is taking the next games.”

Tiparul bun: **x din n + domeniu (acasă, competiție, sezon) + medie în paranteză**. Variante recomandate pentru golstat:
- „Peste 4,5 cornere în 5 din ultimele 7 meciuri acasă (medie 5,9).”
- „Cel puțin un cartonaș în prima repriză în 6 din 7 deplasări (medie 1,4).”
- „Peste 2,5 cornere în repriza 1 în 4 din 7 meciuri acasă · media ligii 48%.”
- „Model 62% · s-a întâmplat 4/7 · liga 51%.”
- „Arbitru: 5,1 cartonașe/meci în carieră (212 meciuri) — peste media ligii (4,3).”
- „Eșantion mic: doar 3 meciuri în această competiție — procent orientativ.”
- De evitat: „sigur”, „garantat”, „pont”. De preferat: „șansă estimată”, „s-a întâmplat în”.

## Ce lipsește în golstat și merită adăugat

Ordine după prioritate. Datele există pentru toate, cu excepțiile notate.

1. **Indicator de eșantion și încredere** (n meciuri, 3 trepte, culoare neutră sub prag) — **S**. Evită ca „3/3” să pară 100%; e esențial pentru filtrul „doar această competiție”.
2. **Media ligii lângă cifra echipei** — **S/M**. Fără reper, „62%” nu spune nimic.
3. **Factor arbitru vizibil** la cartonașe și faulturi: medie galbene/roșii/faulturi pe meci, total de meciuri, comparație cu liga și etichetă „îngăduitor / mediu / strict” (modelul StatsHub) — **S/M**. Avem acum mediile de carieră de la furnizor; atenție că sunt de carieră, nu pe competiție, deci trebuie etichetat așa.
4. **Scară de linii pe aceeași piață** (8.5 / 9.5 / 10.5 pe un rând; pe echipă 3.5 / 4.5 / 5.5) — **S**. Modelul dă deja distribuția.
5. **Pentru / contra pe echipă** (cornere obținute vs cedate, faulturi comise vs suferite, cartonașe) — **S**.
6. **Cornere, faulturi, cartonașe și șuturi pe repriză, pe echipă** — **M**. Datele pe repriză există acum; „cornere echipă în prima repriză” nu apare la niciun competitor verificat.
7. **Propoziții de tendință generate automat** după tiparul din secțiunea 4 — **M**.
8. **Filtru „doar această competiție”** cu avertisment automat sub prag — **M**. Critic pentru Champions/Europa/Conference League 2026/27, unde o echipă are 3–4 meciuri.
9. **Istoric de acuratețe și calibrare** din bifele ✓/✗ („din piețele date la 60–70% s-au realizat 64%”) — **M**. Exact ce i se reproșează lui Forebet că nu are.
10. **Mini-distribuție pe piață** (histogramă a totalurilor recente) — **M**. Răspunde problemei „medie fără distribuție”.
11. **Ajustare la adversar, afișată explicit** („adversarul cedează 5,8 cornere/meci în deplasare”) — **M**.
12. **Context pentru cupele europene**: forța ligii de proveniență, deoarece echipe din campionate diferite au medii incomparabile — **L**. Necesită un coeficient de ligă pe care probabil nu îl avem.
13. **Intervale de 15 minute** pentru goluri și cartonașe — **M**. Evenimentele au minut; pentru cornere **nu avem** minutul.
14. **Cotă corectă (1/p)** lângă procent — **S**. **Valoare față de cota casei** — **L**: **necesită cote pe care nu le avem**.
15. **Dată de actualizare și proveniență pe fiecare card** („din 7 meciuri, actualizat la…”) — **S**. Răspunde direct plângerii „date vechi” de la FootyStats.

## Ce are golstat în plus

- **Probabilitate din model alături de frecvența empirică** („4/7”); competitorii verificați arată de regulă doar una.
- **Verificare ✓/✗ după meci**: bază pentru transparența pe care Forebet nu o oferă.
- **Split acasă/deplasare implicit**, nu ascuns într-un tab.
- **Cornere pe repriză și pe echipă cu linii**: negăsit la niciun produs verificat.
- **Date pe repriză pentru faulturi, cartonașe și șuturi** și profil de arbitru pe fiecare meci.
- **O singură pagină de meci, lizibilă, în română**, fără paywall pe ligă și fără reclame.

## Bune practici pentru probabilități afișate unor nespecialiști

*Recomandări generale; doar punctele despre serii, cote și medii au sursă mai jos.*
- Arată numitorul („4 din 7”), nu doar procentul.
- Sub ~5 meciuri: etichetă „eșantion mic” și culoare neutră.
- Pune reperul ligii lângă cifra echipei.
- Separă vizual „estimare model” de „s-a întâmplat”.
- Nu marca nimic drept „sigur” pe baza unei serii.
- Publică propria calibrare.

## Surse

- https://footystats.org/stats/corner-stats
- https://footystats.org/stats/card-stats
- https://footystats.org/stats/referee-stats
- https://footystats.org/premium
- https://footystats.org/full-guide
- https://footystats.org/academy/betting-on-corners
- https://www.trustpilot.com/review/footystats.org
- https://statshub.com/referees
- https://www.statshub.com/features
- https://statshub.com/blog/betting-on-corners
- https://statshub.com/blog/best-football-stats-websites
- https://corner-stats.com/previews
- https://corner-stats.com/pay
- https://corner-stats.com/goals-and-cards-stats-by-halves-added/info/36
- https://www.flashscore.com/news/more-data-to-help-you-read-the-game-flashscore-s-football-stats-get-a-facelift/42Kj2YYI/
- https://www.sofascore.com/news/football-lineups-and-head-to-head-stats-your-ultimate-match-preview-tool
- https://www.soccerstats.com/ (fragmente de căutare; acces direct 403)
- https://totalcorner.com/league/view/272 (fragmente de căutare)
- https://website.informer.com/adamchoi.co.uk (fragment de căutare)
- https://www.valuethemarkets.com/prediction-markets/forebet-review-how-data-driven-football-forecasts-work-and-their-limits (fragment de căutare)
- https://www.betshoot.com/betting-guides/profitability-and-hit-rate/ (fragment de căutare)
- https://a2i2.deakin.edu.au/2016/09/28/a-closer-look-at-scoring-streaks/ (fragment de căutare)
