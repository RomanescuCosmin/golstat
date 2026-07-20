import type {
  AntetCompetitie,
  AntetEchipa,
  EchipaDeStartDto,
  EchipaDto,
  EchipaFormatie,
  EchipaLineupDto,
  EgaluriDto,
  EvenimentMeci,
  FereastraFormaDto,
  FixtureLive,
  FormaEchipaDto,
  FormaMeciDto,
  FrecventaDto,
  GgDto,
  IntalnireDirectaDto,
  JucatorLineupDto,
  LigaZi,
  LinieGolDto,
  LinieStatDto,
  MeciCentral,
  MeciCompetitie,
  MeciForma,
  MeciLive,
  MeciScurt,
  MeciZiGrupat,
  MediiEchipaDto,
  PaginaCompetitie,
  PaginaEchipa,
  PaginaJucator,
  CotaPiata,
  MeciPiete,
  PiataStatDto,
  PieteZile,
  Predictie1X2,
  PredictieMeciDto,
  PrevizualizareMeciDto,
  ProcentCota,
  Program,
  ProgramLiga,
  ProgramMeci,
  ProgramZi,
  ProgramZiGrupat,
  ZiPiete,
  RandClasament,
  RezultatCautare,
  SezonJucator,
  StatisticiAvansateDto,
  StatisticiCheieDto,
  StatisticiEchipaDto,
  StatisticiEchipaMeci,
  StatisticiLiga,
  StatisticiMeci,
} from '../api/types';

/**
 * Buildere partial-override pentru DTO-urile din `api/types.ts` — echivalentul builder-elor
 * de fixture din testele Java. Valorile implicite sunt realiste si non-null; testele suprascriu
 * explicit cu `null` (= "necolectat") sau valori corupte cand verifica robustetea.
 * Conventii de respectat in override-uri: `ProcentCota.procent` e 0..100, probabilitatile
 * din statisticile avansate sunt fractii 0..1.
 */

export function echipa(over: Partial<EchipaDto> = {}): EchipaDto {
  return { id: 33, nume: 'Manchester United', logo: null, ...over };
}

export function procentCota(procent = 50): ProcentCota {
  return { procent, cota: procent > 0 ? 100 / procent : 0 };
}

export function predictie1X2(g = 45, e = 27, o = 28): Predictie1X2 {
  return { gazde: procentCota(g), egal: procentCota(e), oaspeti: procentCota(o) };
}

export function linieGol(over: Partial<LinieGolDto> = {}): LinieGolDto {
  return { linie: 2.5, peste: procentCota(55), sub: procentCota(45), ...over };
}

export function predictieMeci(over: Partial<PredictieMeciDto> = {}): PredictieMeciDto {
  return {
    fixtureId: 1001,
    echipaGazde: echipa(),
    echipaOaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    kickoff: '2026-07-10T22:00:00+03:00',
    lambdaGazde: 1.6,
    lambdaOaspeti: 1.2,
    gazde: procentCota(45),
    egal: procentCota(27),
    oaspeti: procentCota(28),
    linii: [linieGol()],
    btts: procentCota(52),
    esantionGazde: 7,
    esantionOaspeti: 7,
    rezultat: null,
    ...over,
  };
}

export function meciLive(over: Partial<MeciLive> = {}): MeciLive {
  return {
    fixtureId: 2001,
    leagueId: 39,
    ligaNume: 'Premier League',
    ligaLogo: null,
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    golGazde: 1,
    golOaspeti: 0,
    status: '1H',
    minut: 30,
    ...over,
  };
}

export function fixtureLive(over: Partial<FixtureLive> = {}): FixtureLive {
  return {
    id: 2001,
    statusShort: '2H',
    statusElapsed: 67,
    goalsHome: 2,
    goalsAway: 0,
    homeTeamId: 33,
    awayTeamId: 40,
    kickoff: '2026-07-07T19:00:00+03:00',
    leagueId: 39,
    ...over,
  };
}

export function jucatorLineup(over: Partial<JucatorLineupDto> = {}): JucatorLineupDto {
  return { id: 501, nume: 'A. Onana', numar: 24, pozitie: 'G', grid: '1:1', foto: null, ...over };
}

export function echipaFormatie(over: Partial<EchipaFormatie> = {}): EchipaFormatie {
  return { formatie: '4-3-3', antrenor: 'E. ten Hag', titulari: [jucatorLineup()], rezerve: [], ...over };
}

export function statisticiEchipaMeci(over: Partial<StatisticiEchipaMeci> = {}): StatisticiEchipaMeci {
  return {
    posesie: 55,
    suturiPePoarta: 5,
    suturiTotal: 12,
    cornere: 6,
    faulturi: 10,
    galbene: 2,
    rosii: 0,
    pase: 480,
    paseReusite: 410,
    preciziePase: 85,
    xg: 1.7,
    ...over,
  };
}

export function statisticiMeci(over: Partial<StatisticiMeci> = {}): StatisticiMeci {
  return {
    gazde: statisticiEchipaMeci(),
    oaspeti: statisticiEchipaMeci({ posesie: 45, cornere: 3 }),
    ...over,
  };
}

export function evenimentMeci(over: Partial<EvenimentMeci> = {}): EvenimentMeci {
  return {
    id: 1,
    teamId: 33,
    gazde: true,
    minut: 23,
    minutExtra: null,
    tip: 'Goal',
    detaliu: 'Normal Goal',
    jucator: 'B. Fernandes',
    asist: null,
    ...over,
  };
}

export function meciCentral(over: Partial<MeciCentral> = {}): MeciCentral {
  return {
    fixtureId: 2001,
    leagueId: 39,
    ligaNume: 'Premier League',
    ligaLogo: null,
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    golGazde: 1,
    golOaspeti: 0,
    status: '1H',
    statusLung: 'First Half',
    minut: 30,
    inDesfasurare: true,
    terminat: false,
    kickoff: '2026-07-07T19:00:00+03:00',
    arbitru: 'M. Oliver',
    stadion: 'Old Trafford',
    statistici: statisticiMeci(),
    formatii: { gazde: echipaFormatie(), oaspeti: echipaFormatie({ antrenor: 'A. Slot' }) },
    evenimente: [evenimentMeci()],
    ...over,
  };
}

/* ── Meciurile zilei (prima pagina) ── */

export function meciZi(over: Partial<MeciZiGrupat> = {}): MeciZiGrupat {
  return {
    fixtureId: 3001,
    kickoff: '2026-07-07T22:00:00+03:00',
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    golGazde: null,
    golOaspeti: null,
    status: 'NS',
    inDesfasurare: false,
    terminat: false,
    minut: null,
    runda: 'Regular Season - 12',
    predictie: predictie1X2(),
    ...over,
  };
}

export function ligaZi(over: Partial<LigaZi> = {}): LigaZi {
  return {
    leagueId: 39,
    nume: 'Premier League',
    tara: 'Anglia',
    logo: null,
    meciuri: [meciZi()],
    ...over,
  };
}

export function programZiGrupat(over: Partial<ProgramZiGrupat> = {}): ProgramZiGrupat {
  return { data: '2026-07-07', ligi: [ligaZi()], ...over };
}

/* ── Program (meciuri viitoare) ── */

export function programMeci(over: Partial<ProgramMeci> = {}): ProgramMeci {
  return {
    fixtureId: 4001,
    kickoff: '2026-07-09T22:00:00+03:00',
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    ...over,
  };
}

export function programLiga(over: Partial<ProgramLiga> = {}): ProgramLiga {
  return { leagueId: 39, nume: 'Premier League', tara: 'Anglia', logo: null, meciuri: [programMeci()], ...over };
}

export function programZi(over: Partial<ProgramZi> = {}): ProgramZi {
  return { data: '2026-07-09', ligi: [programLiga()], ...over };
}

export function program(over: Partial<Program> = {}): Program {
  return { zile: [programZi()], ...over };
}

/* ── Previzualizare ── */

export function formaMeci(over: Partial<FormaMeciDto> = {}): FormaMeciDto {
  return { data: '2026-06-28', acasa: true, golMarcate: 2, golPrimite: 1, rezultat: 'V', ...over };
}

export function fereastraForma(over: Partial<FereastraFormaDto> = {}): FereastraFormaDto {
  return {
    meciuri: [formaMeci(), formaMeci({ rezultat: 'E', golMarcate: 1 })],
    goluriMarcatePeMeci: 1.5,
    goluriPrimitePeMeci: 1.0,
    ...over,
  };
}

export function formaEchipa(over: Partial<FormaEchipaDto> = {}): FormaEchipaDto {
  return { locatie: fereastraForma(), general: fereastraForma(), ...over };
}

export function intalnireDirecta(over: Partial<IntalnireDirectaDto> = {}): IntalnireDirectaDto {
  return {
    fixtureId: 900,
    data: '2026-01-15T22:00:00+02:00',
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    golGazde: 2,
    golOaspeti: 2,
    ...over,
  };
}

export function frecventa(reusite = 4, total = 7): FrecventaDto {
  return { reusite, total };
}

export function mediiEchipa(over: Partial<MediiEchipaDto> = {}): MediiEchipaDto {
  return { proprieLocatie: 1.6, totalLocatie: 2.8, proprieGeneral: 1.4, totalGeneral: 2.6, ...over };
}

export function linieStat(over: Partial<LinieStatDto> = {}): LinieStatDto {
  return {
    linie: 2.5,
    probabilitate: 0.55,
    gazdeLocatie: frecventa(),
    gazdeGeneral: frecventa(5, 7),
    oaspetiLocatie: frecventa(3, 7),
    oaspetiGeneral: frecventa(4, 7),
    ...over,
  };
}

export function piataStat(over: Partial<PiataStatDto> = {}): PiataStatDto {
  return { linii: [linieStat()], gazde: mediiEchipa(), oaspeti: mediiEchipa(), ...over };
}

export function gg(over: Partial<GgDto> = {}): GgDto {
  return {
    probabilitate: 0.52,
    gazdeMarcat: frecventa(),
    gazdePrimit: frecventa(3, 7),
    oaspetiMarcat: frecventa(5, 7),
    oaspetiPrimit: frecventa(4, 7),
    ...over,
  };
}

export function egaluri(over: Partial<EgaluriDto> = {}): EgaluriDto {
  return {
    egalPauza: 0.4,
    egalFinal: 0.27,
    pauzaGazde: frecventa(3, 7),
    pauzaOaspeti: frecventa(2, 7),
    finalGazde: frecventa(2, 7),
    finalOaspeti: frecventa(1, 7),
    ...over,
  };
}

export function statisticiAvansate(over: Partial<StatisticiAvansateDto> = {}): StatisticiAvansateDto {
  return {
    goluri: piataStat(),
    gg: gg(),
    cornere: piataStat({ linii: [linieStat({ linie: 9.5 })] }),
    faulturi: piataStat({ linii: [linieStat({ linie: 21.5 })] }),
    cartonase: piataStat({ linii: [linieStat({ linie: 4.5 })] }),
    suturi: piataStat({ linii: [linieStat({ linie: 22.5 })] }),
    suturiPePoarta: piataStat({ linii: [linieStat({ linie: 8.5 })] }),
    egaluri: egaluri(),
    reprize: {
      golRepriza1: 0.7,
      golRepriza2: 0.8,
      repriza1Gazde: frecventa(),
      repriza1Oaspeti: frecventa(5, 7),
      repriza2Gazde: frecventa(6, 7),
      repriza2Oaspeti: frecventa(5, 7),
    },
    rezultat: null,
    ...over,
  };
}

export function statisticiEchipa(over: Partial<StatisticiEchipaDto> = {}): StatisticiEchipaDto {
  return {
    posesieMedie: 55,
    suturiPeMeci: 13.2,
    suturiPePoarta: 5.1,
    cornerePeMeci: 6.4,
    cartonasePeMeci: 2.1,
    ...over,
  };
}

export function statisticiCheie(over: Partial<StatisticiCheieDto> = {}): StatisticiCheieDto {
  return { gazde: statisticiEchipa(), oaspeti: statisticiEchipa({ posesieMedie: 45 }), ...over };
}

export function echipaLineup(over: Partial<EchipaLineupDto> = {}): EchipaLineupDto {
  return { formatie: '4-3-3', titulari: [jucatorLineup()], rezerve: [], indisponibili: [], ...over };
}

export function echipeDeStart(over: Partial<EchipaDeStartDto> = {}): EchipaDeStartDto {
  return { gazde: echipaLineup(), oaspeti: echipaLineup(), arbitru: 'M. Oliver', probabila: true, ...over };
}

export function previzualizareMeci(over: Partial<PrevizualizareMeciDto> = {}): PrevizualizareMeciDto {
  return {
    predictie: predictieMeci(),
    formaGazde: formaEchipa(),
    formaOaspeti: formaEchipa(),
    intalniriDirecte: [intalnireDirecta()],
    statistici: statisticiAvansate(),
    statisticiCheie: statisticiCheie(),
    echipeDeStart: echipeDeStart(),
    ...over,
  };
}

/* ── Echipa / Competitie / Jucator / Cautare / Statistici ── */

export function antetEchipa(over: Partial<AntetEchipa> = {}): AntetEchipa {
  return {
    teamId: 33,
    nume: 'Manchester United',
    logo: null,
    tara: 'Anglia',
    liga: 'Premier League',
    ligaLogo: null,
    leagueId: 39,
    sezon: 2025,
    antrenor: 'E. ten Hag',
    stadion: 'Old Trafford',
    capacitate: 76000,
    ...over,
  };
}

export function meciForma(over: Partial<MeciForma> = {}): MeciForma {
  return {
    fixtureId: 5001,
    data: '2026-06-28T22:00:00+03:00',
    acasa: true,
    adversar: echipa({ id: 40, nume: 'Liverpool' }),
    golMarcate: 2,
    golPrimite: 1,
    rezultat: 'V',
    liga: 'Premier League',
    ligaLogo: null,
    runda: 'Regular Season - 12',
    ...over,
  };
}

export function meciScurt(over: Partial<MeciScurt> = {}): MeciScurt {
  return {
    fixtureId: 6001,
    kickoff: '2026-07-12T22:00:00+03:00',
    adversar: echipa({ id: 40, nume: 'Liverpool' }),
    acasa: true,
    liga: 'Premier League',
    ligaLogo: null,
    runda: 'Regular Season - 13',
    ...over,
  };
}

export function randClasament(over: Partial<RandClasament> = {}): RandClasament {
  return {
    rank: 1,
    teamId: 33,
    nume: 'Manchester United',
    logo: null,
    jucate: 12,
    victorii: 8,
    egaluri: 2,
    infrangeri: 2,
    golaveraj: 10,
    puncte: 26,
    echipaCurenta: false,
    ...over,
  };
}

export function paginaEchipa(over: Partial<PaginaEchipa> = {}): PaginaEchipa {
  return {
    antet: antetEchipa(),
    sumar: {
      pozitie: 3,
      puncte: 26,
      jucate: 12,
      victorii: 8,
      egaluri: 2,
      infrangeri: 2,
      goluriMarcate: 24,
      goluriPrimite: 14,
      golaveraj: 10,
    },
    forma: [meciForma()],
    rezultateRecente: [meciForma()],
    statProcente: [{ categorie: 'GOLURI', medieEchipa: 2.0, medieLiga: 2.8, procent: 36 }],
    sezoane: [2025, 2024],
    urmatorulMeci: meciScurt(),
    clasament: [randClasament(), randClasament({ rank: 2, teamId: 40, nume: 'Liverpool', puncte: 25 })],
    statistici: {
      goluriMarcatePeMeci: 2.0,
      goluriPrimitePeMeci: 1.2,
      cleanSheets: 4,
      galbene: 22,
      rosii: 1,
      suturiPeMeci: 13.2,
      posesieMedie: 55,
      preciziePase: 85,
    },
    goluriPeInterval: [{ interval: '0-15', marcate: 3, primite: 1 }],
    topJucatori: {
      golgheter: { playerId: 909, nume: 'M. Rashford', foto: null, valoare: 9 },
      pasator: null,
      minute: null,
      galbene: null,
      rosii: null,
    },
    ...over,
  };
}

export function meciCompetitie(over: Partial<MeciCompetitie> = {}): MeciCompetitie {
  return {
    fixtureId: 7001,
    kickoff: '2026-07-09T22:00:00+03:00',
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    golGazde: null,
    golOaspeti: null,
    status: 'NS',
    inDesfasurare: false,
    terminat: false,
    runda: null,
    ...over,
  };
}

export function antetCompetitie(over: Partial<AntetCompetitie> = {}): AntetCompetitie {
  return { leagueId: 39, nume: 'Premier League', tara: 'Anglia', logo: null, sezon: 2025, sezoane: [2025, 2024], ...over };
}

export function paginaCompetitie(over: Partial<PaginaCompetitie> = {}): PaginaCompetitie {
  return {
    antet: antetCompetitie(),
    clasament: [randClasament(), randClasament({ rank: 2, teamId: 40, nume: 'Liverpool', puncte: 25 })],
    golgheteri: [{ playerId: 909, nume: 'M. Rashford', foto: null, echipa: echipa(), valoare: 9 }],
    pasatori: [{ playerId: 910, nume: 'B. Fernandes', foto: null, echipa: echipa(), valoare: 7 }],
    rezultate: [meciCompetitie({ golGazde: 2, golOaspeti: 1, status: 'FT', terminat: true })],
    urmatoare: [meciCompetitie()],
    grupe: [],
    eliminatorii: [],
    ...over,
  };
}

export function sezonJucator(over: Partial<SezonJucator> = {}): SezonJucator {
  return {
    leagueId: 39,
    liga: 'Premier League',
    ligaLogo: null,
    sezon: 2025,
    echipa: echipa(),
    aparitii: 20,
    minute: 1650,
    goluri: 9,
    pase: 4,
    galbene: 3,
    rosii: 0,
    rating: 7.4,
    ...over,
  };
}

export function paginaJucator(over: Partial<PaginaJucator> = {}): PaginaJucator {
  return {
    playerId: 909,
    nume: 'M. Rashford',
    foto: null,
    nationalitate: 'Anglia',
    varsta: 28,
    pozitie: 'Attacker',
    echipaCurenta: echipa(),
    sezoane: [sezonJucator()],
    ...over,
  };
}

export function rezultatCautare(over: Partial<RezultatCautare> = {}): RezultatCautare {
  return { tip: 'ECHIPA', id: 33, nume: 'Manchester United', imagine: null, subtitlu: 'Anglia', nationala: false, ...over };
}

export function statisticiLiga(over: Partial<StatisticiLiga> = {}): StatisticiLiga {
  return {
    leagueId: 39,
    nume: 'Premier League',
    tara: 'Anglia',
    logo: null,
    sezon: 2025,
    medieGoluri: 2.8,
    medieCornere: 10.2,
    medieFaulturi: 21.5,
    medieCartonase: 4.1,
    ...over,
  };
}

/* ─────────────────────────── Piețe pe zile ─────────────────────────── */

/** `probabilitate` e fracție 0..1 (nu procent) — vezi nota din capul fișierului. */
export function cotaPiata(over: Partial<CotaPiata> = {}): CotaPiata {
  const probabilitate = over.probabilitate ?? 0.62;
  return {
    piata: 'GOLURI_PESTE',
    linie: 2.5,
    probabilitate,
    cota: probabilitate > 0 ? Math.round(100 / probabilitate) / 100 : 0,
    esantion: 14,
    ...over,
  };
}

export function meciPiete(over: Partial<MeciPiete> = {}): MeciPiete {
  return {
    fixtureId: 5001,
    kickoff: '2026-07-21T18:00:00Z',
    liga: { id: 39, nume: 'Premier League', logo: null },
    gazde: echipa(),
    oaspeti: echipa({ id: 40, nume: 'Liverpool' }),
    piete: [cotaPiata()],
    ...over,
  };
}

export function ziPiete(over: Partial<ZiPiete> = {}): ZiPiete {
  return { data: '2026-07-21', meciuri: [meciPiete()], ...over };
}

export function pieteZile(over: Partial<PieteZile> = {}): PieteZile {
  return { zile: [ziPiete()], ...over };
}
