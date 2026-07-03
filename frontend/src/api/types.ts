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

/** Forma recenta a unei echipe: ultimele meciuri (cele mai recente primele) + medii de goluri. */
export interface FormaEchipaDto {
  meciuri: FormaMeciDto[];
  goluriMarcatePeMeci: number;
  goluriPrimitePeMeci: number;
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

/** Un jucator din formatie; `grid` = pozitia in teren "rand:coloana" (null la rezerve). */
export interface JucatorLineupDto {
  id: number | null;
  nume: string | null;
  numar: number | null;
  pozitie: string | null;
  grid: string | null;
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
}

/** Oglinda `ro.golstat.api.preview.PrevizualizareMeciDto`. */
export interface PrevizualizareMeciDto {
  predictie: PredictieMeciDto;
  formaGazde: FormaEchipaDto;
  formaOaspeti: FormaEchipaDto;
  intalniriDirecte: IntalnireDirectaDto[];
  cornere: OverUnder[];
  faulturi: OverUnder[];
  cartonase: OverUnder[];
  statisticiCheie: StatisticiCheieDto;
  /** `null` pana se anunta formatiile, aproape de meci. */
  echipeDeStart: EchipaDeStartDto | null;
}
