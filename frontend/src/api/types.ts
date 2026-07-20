/**
 * Oglinda TypeScript a DTO-urilor Java expuse de modulul `api`.
 * Atentie la cele doua conventii diferite:
 *  - `ProcentCota.procent` este procent 0..100 (plus cota statistica 1/p);
 *  - `OverUnder.overRate` / `underRate` sunt fractii 0..1 (cornere, faulturi, cartonase).
 */

/** O echipa pentru afisare; nume/logo pot lipsi daca echipa nu e in DB. */
export interface EchipaDto {
  id: number;
  nume: string | null;
  logo: string | null;
}

/** O piata: procentul (0..100) si cota statistica derivata (1/p). */
export interface ProcentCota {
  procent: number;
  cota: number;
}

/** O linie over/under de goluri (ex. 2.5) cu ambele parti, in procente 0..100. */
export interface LinieGolDto {
  linie: number;
  peste: ProcentCota;
  sub: ProcentCota;
}

/**
 * Scorul real la 90 min al unui meci terminat, pentru validarea predictiei; `null` la meciuri
 * viitoare. `statusShort`: "FT" / "AET" / "PEN".
 */
export interface RezultatMeciDto {
  goluriGazde: number;
  goluriOaspeti: number;
  statusShort: string;
}

/** Oglinda `ro.golstat.api.prediction.PredictieMeciDto`. */
export interface PredictieMeciDto {
  fixtureId: number;
  echipaGazde: EchipaDto;
  echipaOaspeti: EchipaDto;
  /** OffsetDateTime serializat ISO-8601, ex. "2026-07-03T22:00:00+03:00". */
  kickoff: string;
  lambdaGazde: number;
  lambdaOaspeti: number;
  gazde: ProcentCota;
  egal: ProcentCota;
  oaspeti: ProcentCota;
  linii: LinieGolDto[];
  btts: ProcentCota;
  esantionGazde: number;
  esantionOaspeti: number;
  /** Scorul real la 90 min; `null` la meciuri viitoare (NS). */
  rezultat: RezultatMeciDto | null;
}

/**
 * Oglinda `ro.golstat.stats.market.OverUnder` — o linie x.5 pentru un total de meci
 * (cornere, faulturi, cartonase). Ratele sunt FRACTII 0..1, `overRate + underRate == 1`.
 */
export interface OverUnder {
  line: number;
  overRate: number;
  underRate: number;
}

/** Un meci din forma recenta a unei echipe; `rezultat`: "V" / "E" / "I". */
export interface FormaMeciDto {
  /** LocalDate serializat "YYYY-MM-DD". */
  data: string;
  acasa: boolean;
  golMarcate: number;
  golPrimite: number;
  rezultat: 'V' | 'E' | 'I';
}

/** O fereastra de forma: ultimele meciuri (cele mai recente primele) + medii de goluri. */
export interface FereastraFormaDto {
  meciuri: FormaMeciDto[];
  goluriMarcatePeMeci: number;
  goluriPrimitePeMeci: number;
}

/**
 * Forma recenta a unei echipe pe doua ferestre de cate 7: `locatie` = pe locatia din meciul
 * previzualizat (gazdele acasa, oaspetii in deplasare), `general` = indiferent de locatie.
 */
export interface FormaEchipaDto {
  locatie: FereastraFormaDto;
  general: FereastraFormaDto;
}

/** O intalnire directa din trecut; gazdele/oaspetii sunt cei ai meciului ISTORIC. */
export interface IntalnireDirectaDto {
  fixtureId: number;
  /** OffsetDateTime serializat ISO-8601. */
  data: string;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  golGazde: number | null;
  golOaspeti: number | null;
}

/** Mediile unei echipe; `null` = fara date colectate pentru acel camp (diferit de 0). */
export interface StatisticiEchipaDto {
  posesieMedie: number | null;
  suturiPeMeci: number | null;
  suturiPePoarta: number | null;
  cornerePeMeci: number | null;
  cartonasePeMeci: number | null;
}

/** Oglinda `ro.golstat.api.preview.StatisticiCheieDto`. */
export interface StatisticiCheieDto {
  gazde: StatisticiEchipaDto;
  oaspeti: StatisticiEchipaDto;
}

/**
 * Oglinda campurilor relevante din `ro.golstat.common.dto.FixtureDto`, primit pe
 * WebSocket (`/topic/live/{fixtureId}`) cand meciul e in desfasurare.
 * `statusShort` in-play: "1H" / "HT" / "2H" / "ET" / "P".
 */
export interface FixtureLive {
  id: number;
  statusShort: string | null;
  statusElapsed: number | null;
  goalsHome: number | null;
  goalsAway: number | null;
  homeTeamId: number | null;
  awayTeamId: number | null;
  /** OffsetDateTime serializat ISO-8601. */
  kickoff: string | null;
  leagueId: number | null;
}

/** Oglinda `ro.golstat.api.live.MeciLiveDto` — un meci in desfasurare (din DB, prin endpoint-ul /live). */
export interface MeciLive {
  fixtureId: number;
  leagueId: number | null;
  ligaNume: string | null;
  ligaLogo: string | null;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  golGazde: number | null;
  golOaspeti: number | null;
  /** status in-play: "1H" / "HT" / "2H" / "ET" / "P". */
  status: string | null;
  minut: number | null;
}

/** Un jucator din formatie; `grid` = pozitia in teren "rand:coloana" (null la rezerve). */
export interface JucatorLineupDto {
  id: number | null;
  nume: string | null;
  numar: number | null;
  pozitie: string | null;
  grid: string | null;
  /** URL-ul fotografiei de profil; null cand nu exista (fallback la cercul cu numar). */
  foto: string | null;
}

/** Un jucator indisponibil; `detaliu` = motivul brut din sursa (ex. "Knee Injury"). */
export interface IndisponibilDto {
  id: number;
  nume: string | null;
  motiv: 'ACCIDENTAT' | 'SUSPENDAT' | 'INCERT';
  detaliu: string | null;
}

/** Formatia unei echipe: titulari + rezerve + indisponibili. */
export interface EchipaLineupDto {
  formatie: string | null;
  titulari: JucatorLineupDto[];
  rezerve: JucatorLineupDto[];
  indisponibili: IndisponibilDto[];
}

/** Oglinda `ro.golstat.api.preview.EchipaDeStartDto`. */
export interface EchipaDeStartDto {
  gazde: EchipaLineupDto;
  oaspeti: EchipaLineupDto;
  arbitru: string | null;
  /** true = echipa PROBABILA (ultimul unsprezece), nu formatia anuntata a meciului. */
  probabila: boolean;
}

/* ──────────────── Statistici avansate (analiza pe piete, ferestre de 7) ──────────────── */

/** "In `reusite` din `total` meciuri" — materia prima a legendei; `total` 0 = fara date. */
export interface FrecventaDto {
  reusite: number;
  total: number;
}

/** Mediile pe meci ale unei echipe pe piata; `proprie*` = doar echipa, `total*` = ambele parti. */
export interface MediiEchipaDto {
  proprieLocatie: number | null;
  totalLocatie: number | null;
  proprieGeneral: number | null;
  totalGeneral: number | null;
}

/** O linie x.5: probabilitatea MODELATA (0..1) pe meci + frecventele empirice per fereastra. */
export interface LinieStatDto {
  linie: number;
  probabilitate: number;
  gazdeLocatie: FrecventaDto;
  gazdeGeneral: FrecventaDto;
  oaspetiLocatie: FrecventaDto;
  oaspetiGeneral: FrecventaDto;
}

/** O piata cu linii Over/Under si mediile fiecarei echipe. */
export interface PiataStatDto {
  linii: LinieStatDto[];
  gazde: MediiEchipaDto;
  oaspeti: MediiEchipaDto;
}

/** GG (ambele marcheaza): probabilitate modelata + marcat/primit pe ferestrele de locatie. */
export interface GgDto {
  probabilitate: number;
  gazdeMarcat: FrecventaDto;
  gazdePrimit: FrecventaDto;
  oaspetiMarcat: FrecventaDto;
  oaspetiPrimit: FrecventaDto;
}

/** Egal la pauza / final (rate 0..1) + frecventele fiecarei echipe pe fereastra de locatie. */
export interface EgaluriDto {
  egalPauza: number;
  egalFinal: number;
  pauzaGazde: FrecventaDto;
  pauzaOaspeti: FrecventaDto;
  finalGazde: FrecventaDto;
  finalOaspeti: FrecventaDto;
}

/** Se marcheaza in repriza 1 / 2 (rate 0..1) + frecventele empirice. */
export interface ReprizeDto {
  golRepriza1: number;
  golRepriza2: number;
  repriza1Gazde: FrecventaDto;
  repriza1Oaspeti: FrecventaDto;
  repriza2Gazde: FrecventaDto;
  repriza2Oaspeti: FrecventaDto;
}

/**
 * Totalurile reale ale meciului pe fiecare piata, pentru a marca hit/miss fata de partea favorizata
 * de model; `null` la meciuri viitoare. Golurile sunt la 90 min; campurile de repriza sunt `null`
 * cand scorul la pauza lipseste; totalurile de count sunt `null` cand meciul n-are statistici.
 */
export interface RezultatStatisticiDto {
  totalGoluri: number;
  ambeleMarcheaza: boolean;
  egalFinal: boolean;
  egalPauza: boolean | null;
  golRepriza1: boolean | null;
  golRepriza2: boolean | null;
  totalCornere: number | null;
  totalFaulturi: number | null;
  totalCartonase: number | null;
  totalSuturi: number | null;
  totalSuturiPePoarta: number | null;
}

/** Oglinda `ro.golstat.api.preview.StatisticiAvansateDto`. */
export interface StatisticiAvansateDto {
  goluri: PiataStatDto;
  gg: GgDto;
  cornere: PiataStatDto;
  faulturi: PiataStatDto;
  cartonase: PiataStatDto;
  suturi: PiataStatDto;
  suturiPePoarta: PiataStatDto;
  /** `null` cand niciuna dintre echipe nu are istoric. */
  egaluri: EgaluriDto | null;
  reprize: ReprizeDto | null;
  /** Totalurile reale ale meciului; `null` la meciuri viitoare. */
  rezultat: RezultatStatisticiDto | null;
}

/** Oglinda `ro.golstat.api.preview.PrevizualizareMeciDto`. */
export interface PrevizualizareMeciDto {
  predictie: PredictieMeciDto;
  formaGazde: FormaEchipaDto;
  formaOaspeti: FormaEchipaDto;
  intalniriDirecte: IntalnireDirectaDto[];
  statistici: StatisticiAvansateDto;
  statisticiCheie: StatisticiCheieDto;
  /** `null` pana exista macar o formatie per echipa. */
  echipeDeStart: EchipaDeStartDto | null;
}

/* ─────────────────────────── Match Center ─────────────────────────── */

/** Statisticile REALE ale unei echipe intr-un meci (din fixture_team_stats); `null` = necolectat. */
export interface StatisticiEchipaMeci {
  posesie: number | null;
  suturiPePoarta: number | null;
  suturiTotal: number | null;
  cornere: number | null;
  faulturi: number | null;
  galbene: number | null;
  rosii: number | null;
  pase: number | null;
  paseReusite: number | null;
  preciziePase: number | null;
  xg: number | null;
}

/** Statisticile ambelor echipe pentru un meci; `null` = necolectat pentru echipa respectiva. */
export interface StatisticiMeci {
  gazde: StatisticiEchipaMeci | null;
  oaspeti: StatisticiEchipaMeci | null;
}

/** Un eveniment din cronologia meciului; `tip`: "Goal" / "Card" / "subst" / "Var". */
export interface EvenimentMeci {
  id: number | null;
  teamId: number | null;
  gazde: boolean;
  minut: number | null;
  minutExtra: number | null;
  tip: string | null;
  detaliu: string | null;
  jucator: string | null;
  asist: string | null;
}

/** Formatia unei echipe intr-un meci: schema, antrenor, titulari + rezerve. */
export interface EchipaFormatie {
  formatie: string | null;
  antrenor: string | null;
  titulari: JucatorLineupDto[];
  rezerve: JucatorLineupDto[];
}

/** Formatiile ambelor echipe; `null` pana exista lineup pentru amandoua. */
export interface Formatii {
  gazde: EchipaFormatie;
  oaspeti: EchipaFormatie;
}

/** Oglinda `ro.golstat.api.matchcenter.MeciCentralDto` — detaliul unui meci (live sau finalizat). */
export interface MeciCentral {
  fixtureId: number;
  leagueId: number | null;
  ligaNume: string | null;
  ligaLogo: string | null;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  golGazde: number | null;
  golOaspeti: number | null;
  status: string | null;
  statusLung: string | null;
  minut: number | null;
  inDesfasurare: boolean;
  terminat: boolean;
  /** OffsetDateTime serializat ISO-8601. */
  kickoff: string | null;
  arbitru: string | null;
  stadion: string | null;
  /** `null` daca nu exista statistici colectate pentru meci. */
  statistici: StatisticiMeci | null;
  /** `null` pana exista formatiile ambelor echipe. */
  formatii: Formatii | null;
  evenimente: EvenimentMeci[];
}

/** Tipul unui rezultat din cautarea globala. */
export type TipRezultat = 'ECHIPA' | 'LIGA' | 'JUCATOR';

/** Oglinda `ro.golstat.api.cautare.RezultatCautareDto` — un rezultat din cautarea globala. */
export interface RezultatCautare {
  tip: TipRezultat;
  id: number;
  nume: string | null;
  imagine: string | null;
  subtitlu: string | null;
  nationala: boolean;
}

/* ─────────────────────────── Program ─────────────────────────── */

/** Un meci viitor din program. */
export interface ProgramMeci {
  fixtureId: number;
  /** OffsetDateTime serializat ISO-8601. */
  kickoff: string;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
}

/** O competitie dintr-o zi de program cu meciurile ei. */
export interface ProgramLiga {
  leagueId: number;
  nume: string | null;
  tara: string | null;
  logo: string | null;
  meciuri: ProgramMeci[];
}

/** O zi de program (UTC) cu competitiile care au meciuri in ea. */
export interface ProgramZi {
  /** LocalDate serializat "YYYY-MM-DD". */
  data: string;
  ligi: ProgramLiga[];
}

/** Oglinda `ro.golstat.api.live.ProgramDto` — meciuri viitoare grupate pe zi/competitie. */
export interface Program {
  zile: ProgramZi[];
}

/* ─────────────────────────── Meciurile zilei (prima pagina) ─────────────────────────── */

/** Sansele 1X2 pentru bara de probabilitate; `null` cand nu se poate calcula. */
export interface Predictie1X2 {
  gazde: ProcentCota;
  egal: ProcentCota;
  oaspeti: ProcentCota;
}

/** Un meci dintr-o zi (orice status): scor + status + predictie 1X2, ca sa fie randat direct. */
export interface MeciZiGrupat {
  fixtureId: number;
  /** OffsetDateTime serializat ISO-8601. */
  kickoff: string;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  golGazde: number | null;
  golOaspeti: number | null;
  status: string | null;
  inDesfasurare: boolean;
  terminat: boolean;
  minut: number | null;
  /** Runda competitiei (text liber din sursa, ex. "Regular Season - 12"); poate lipsi. */
  runda: string | null;
  /** Predictia 1X2 pentru bara de probabilitate; `null` cand nu se poate calcula (ex. fara istoric). */
  predictie: Predictie1X2 | null;
}

/** O competitie dintr-o zi cu meciurile ei; nume/tara/logo pot lipsi daca liga nu e in DB. */
export interface LigaZi {
  leagueId: number;
  nume: string | null;
  tara: string | null;
  logo: string | null;
  meciuri: MeciZiGrupat[];
}

/** Oglinda `ro.golstat.api.live.ProgramZiDto` — meciurile unei zile grupate pe competitie. */
export interface ProgramZiGrupat {
  /** LocalDate serializat "YYYY-MM-DD". */
  data: string;
  ligi: LigaZi[];
}

/* ─────────────────────────── Pagina Echipa ─────────────────────────── */

export interface AntetEchipa {
  teamId: number;
  nume: string | null;
  logo: string | null;
  tara: string | null;
  liga: string | null;
  ligaLogo: string | null;
  leagueId: number | null;
  sezon: number | null;
  antrenor: string | null;
  stadion: string | null;
  capacitate: number | null;
}

export interface SumarSezon {
  pozitie: number | null;
  puncte: number | null;
  jucate: number | null;
  victorii: number | null;
  egaluri: number | null;
  infrangeri: number | null;
  goluriMarcate: number | null;
  goluriPrimite: number | null;
  golaveraj: number | null;
}

/** Un meci din forma recenta a echipei; `rezultat`: "V" / "E" / "I". */
export interface MeciForma {
  fixtureId: number;
  /** OffsetDateTime serializat ISO-8601. */
  data: string;
  acasa: boolean;
  adversar: EchipaDto;
  golMarcate: number | null;
  golPrimite: number | null;
  rezultat: 'V' | 'E' | 'I';
  liga: string | null;
  ligaLogo: string | null;
  runda: string | null;
}

export interface MeciScurt {
  fixtureId: number;
  /** OffsetDateTime serializat ISO-8601. */
  kickoff: string;
  adversar: EchipaDto;
  acasa: boolean;
  liga: string | null;
  ligaLogo: string | null;
  runda: string | null;
}

export interface RandClasament {
  rank: number | null;
  teamId: number;
  nume: string | null;
  logo: string | null;
  jucate: number | null;
  victorii: number | null;
  egaluri: number | null;
  infrangeri: number | null;
  golaveraj: number | null;
  puncte: number | null;
  echipaCurenta: boolean;
}

export interface StatBareSezon {
  goluriMarcatePeMeci: number | null;
  goluriPrimitePeMeci: number | null;
  cleanSheets: number | null;
  galbene: number | null;
  rosii: number | null;
  suturiPeMeci: number | null;
  posesieMedie: number | null;
  preciziePase: number | null;
}

/** Un interval de 15 minute cu golurile marcate si primite in el; `interval`: "0-15" … "90+". */
export interface BucketGoluri {
  interval: string;
  marcate: number;
  primite: number;
}

/** Un jucator cu o valoare-cheie (goluri / pase decisive / minute / cartonase). */
export interface JucatorStat {
  playerId: number | null;
  nume: string | null;
  foto: string | null;
  valoare: number;
}

export interface TopJucatori {
  golgheter: JucatorStat | null;
  pasator: JucatorStat | null;
  minute: JucatorStat | null;
  galbene: JucatorStat | null;
  rosii: JucatorStat | null;
}

/** O categorie de statistici cu procent relativ la media ligii (50% = media ligii). */
export interface StatProcent {
  categorie: 'GOLURI' | 'CORNERE' | 'FAULTURI' | 'CARTONASE';
  medieEchipa: number | null;
  medieLiga: number | null;
  procent: number;
}

/** Oglinda `ro.golstat.api.team.PaginaEchipaDto`. Fiecare bloc poate lipsi independent. */
export interface PaginaEchipa {
  antet: AntetEchipa;
  sumar: SumarSezon | null;
  forma: MeciForma[];
  rezultateRecente: MeciForma[];
  statProcente: StatProcent[];
  sezoane: number[];
  urmatorulMeci: MeciScurt | null;
  clasament: RandClasament[];
  statistici: StatBareSezon | null;
  goluriPeInterval: BucketGoluri[];
  topJucatori: TopJucatori | null;
}

/* ─────────────────────────── Competiție ─────────────────────────── */

/** Un jucător dintr-un top al competiției (golgheteri/pasatori) cu echipa lui. */
export interface JucatorTop {
  playerId: number | null;
  nume: string | null;
  foto: string | null;
  echipa: EchipaDto;
  valoare: number;
}

/** Un meci al competiției (rezultat sau program). */
export interface MeciCompetitie {
  fixtureId: number;
  /** OffsetDateTime serializat ISO-8601. */
  kickoff: string;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  golGazde: number | null;
  golOaspeti: number | null;
  status: string | null;
  inDesfasurare: boolean;
  terminat: boolean;
  /** Eticheta etapei (ex. „Round of 16", „Final") — folosita la schema fazelor eliminatorii. */
  runda: string | null;
}

/** O grupa dintr-o competitie cu format de grupe (ex. Cupa Mondiala): numele grupei + clasamentul ei. */
export interface GrupaClasament {
  nume: string;
  randuri: RandClasament[];
}

/** O faza eliminatorie (ex. „Optimi", „Sferturi") cu meciurile ei; fazele vin in ordinea progresiei. */
export interface FazaEliminatorie {
  runda: string;
  meciuri: MeciCompetitie[];
}

/** Antetul unei competiții. */
export interface AntetCompetitie {
  leagueId: number;
  nume: string | null;
  tara: string | null;
  logo: string | null;
  sezon: number | null;
  sezoane: number[];
}

/** Oglinda `ro.golstat.api.competitie.PaginaCompetitieDto`. */
export interface PaginaCompetitie {
  antet: AntetCompetitie;
  clasament: RandClasament[];
  golgheteri: JucatorTop[];
  pasatori: JucatorTop[];
  rezultate: MeciCompetitie[];
  urmatoare: MeciCompetitie[];
  /** Clasamentul spart pe grupe (gol la ligile obisnuite; populat la Cupa Mondiala). */
  grupe: GrupaClasament[];
  /** Schema fazelor eliminatorii (gol la ligile obisnuite; populat la Cupa Mondiala). */
  eliminatorii: FazaEliminatorie[];
}

/* ─────────────────────────── Statistici (ligi) ─────────────────────────── */

/** Oglinda `ro.golstat.api.statistici.StatisticiLigaDto` — mediile pe meci ale unei ligi. */
export interface StatisticiLiga {
  leagueId: number;
  nume: string | null;
  tara: string | null;
  logo: string | null;
  sezon: number | null;
  medieGoluri: number | null;
  medieCornere: number | null;
  medieFaulturi: number | null;
  medieCartonase: number | null;
}

/* ─────────────────────────── Jucător ─────────────────────────── */

/** O linie de statistici a jucătorului într-o ligă/sezon la o echipă. */
export interface SezonJucator {
  leagueId: number;
  liga: string | null;
  ligaLogo: string | null;
  sezon: number | null;
  echipa: EchipaDto | null;
  aparitii: number | null;
  minute: number | null;
  goluri: number | null;
  pase: number | null;
  galbene: number | null;
  rosii: number | null;
  rating: number | null;
}

/** Oglinda `ro.golstat.api.jucator.PaginaJucatorDto`. */
export interface PaginaJucator {
  playerId: number;
  nume: string | null;
  foto: string | null;
  nationalitate: string | null;
  varsta: number | null;
  pozitie: string | null;
  echipaCurenta: EchipaDto | null;
  sezoane: SezonJucator[];
}

/* ─────────────────────────── Piețe pe zile ─────────────────────────── */

/** Codurile de piață expuse de `/v1/piete/zile` (oglinda `ro.golstat.api.piete.CodPiata`). */
export type CodPiata =
  | 'GOLURI_PESTE'
  | 'GOLURI_SUB'
  | 'GG'
  | 'NG'
  | 'CORNERE_PESTE'
  | 'FAULTURI_PESTE'
  | 'CARTONASE_PESTE'
  | 'EGAL_PAUZA'
  | 'EGAL_FINAL';

/**
 * O piață a unui meci. ATENȚIE: `probabilitate` e FRACȚIE 0..1 (ca `LinieStatDto.probabilitate`,
 * NU ca `ProcentCota.procent`) — se formatează cu `formatRata`. `linie` e `null` la piețele binare.
 * `esantion` = câte meciuri stau în spate; serverul nu trimite niciodată piețe cu eșantion 0.
 */
export interface CotaPiata {
  piata: CodPiata;
  linie: number | null;
  probabilitate: number;
  cota: number;
  esantion: number;
}

export interface LigaPiete {
  id: number;
  nume: string | null;
  logo: string | null;
}

export interface MeciPiete {
  fixtureId: number;
  kickoff: string;
  liga: LigaPiete;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  piete: CotaPiata[];
}

export interface ZiPiete {
  data: string;
  meciuri: MeciPiete[];
}

/** Oglinda `ro.golstat.api.piete.PieteZileDto`. */
export interface PieteZile {
  zile: ZiPiete[];
}
