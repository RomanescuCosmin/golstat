import type { MeciCentral, MeciLive, PaginaCompetitie, PaginaEchipa, PaginaJucator, PredictieMeciDto, PrevizualizareMeciDto, Program, ProgramZiGrupat, RezultatCautare, StatisticiLiga } from './types';

const BASE = '/api';

/** Eroare HTTP a API-ului, construita din corpul RFC 7807 (ProblemDetail) cand exista. */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string | null;

  constructor(status: number, title: string, detail: string | null) {
    super(detail ?? title);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
  }
}

interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}

async function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, { headers: { Accept: 'application/json' }, signal });
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e;
    throw new ApiError(0, 'Conexiune eșuată', 'Serverul nu a putut fi contactat.');
  }

  if (!res.ok) {
    let status = res.status;
    let title = `Eroare ${res.status}`;
    let detail: string | null = null;
    try {
      const body = (await res.json()) as ProblemDetail;
      if (typeof body.status === 'number') status = body.status;
      if (body.title) title = body.title;
      if (body.detail) detail = body.detail;
    } catch {
      // corp gol sau non-JSON — pastram mesajul generic
    }
    throw new ApiError(status, title, detail);
  }

  return (await res.json()) as T;
}

/** Predictiile meciurilor viitoare ale unei ligi intr-o zi; `data` in format "YYYY-MM-DD". */
export function getPredictiiZi(leagueId: number, data: string): Promise<PredictieMeciDto[]> {
  const params = new URLSearchParams({ leagueId: String(leagueId), data });
  return request<PredictieMeciDto[]>(`/v1/predictii/meciuri?${params.toString()}`);
}

/** Previzualizarea completa a unui meci viitor. */
export function getPrevizualizare(fixtureId: number): Promise<PrevizualizareMeciDto> {
  return request<PrevizualizareMeciDto>(`/v1/predictii/meciuri/${fixtureId}/previzualizare`);
}

/** Programul meciurilor viitoare (toate competitiile) pe urmatoarele `zile` (1..14), grupat pe zi/liga. */
export function getProgram(zile = 7): Promise<Program> {
  return request<Program>(`/v1/meciuri/urmatoare?zile=${zile}`);
}

/** Meciurile unei zile (`data` "YYYY-MM-DD", implicit azi) din toate ligile europene, grupate pe competitie. */
export function getMeciuriZi(data?: string): Promise<ProgramZiGrupat> {
  const qs = data ? `?data=${data}` : '';
  return request<ProgramZiGrupat>(`/v1/meciuri/zi${qs}`);
}

/** Meciurile in desfasurare acum (orice competitie), din DB. */
export function getLive(): Promise<MeciLive[]> {
  return request<MeciLive[]>('/v1/meciuri/live');
}

/** Detaliul unui meci (scor, statistici, cronologie) — functioneaza pentru meciuri live si finalizate. */
export function getMatchCenter(fixtureId: number): Promise<MeciCentral> {
  return request<MeciCentral>(`/v1/meciuri/${fixtureId}`);
}

/** Cautare globala (echipe, campionate, jucatori; min 2 caractere); `signal` anuleaza cererile stale. */
export function cauta(q: string, signal?: AbortSignal): Promise<RezultatCautare[]> {
  return request<RezultatCautare[]>(`/v1/cauta?q=${encodeURIComponent(q)}`, signal);
}

/** Pagina unei echipe: antet, sumar sezon, forma, clasament, statistici, top jucatori. */
export function getEchipa(teamId: number, leagueId?: number, sezon?: number): Promise<PaginaEchipa> {
  const params = new URLSearchParams();
  if (leagueId != null) params.set('leagueId', String(leagueId));
  if (sezon != null) params.set('sezon', String(sezon));
  const qs = params.toString();
  return request<PaginaEchipa>(`/v1/echipe/${teamId}${qs ? `?${qs}` : ''}`);
}

/** Pagina unei competiții: clasament, golgheteri/pasatori, rezultate, program. `sezon` opțional (default = ultimul jucat). */
export function getCompetitie(leagueId: number, sezon?: number): Promise<PaginaCompetitie> {
  const qs = sezon != null ? `?sezon=${sezon}` : '';
  return request<PaginaCompetitie>(`/v1/competitii/${leagueId}${qs}`);
}

/** Clasamentul de tendințe pe ligi (medii goluri/cornere/faulturi/cartonașe) pentru pagina Statistici. */
export function getStatisticiLigi(): Promise<StatisticiLiga[]> {
  return request<StatisticiLiga[]>('/v1/statistici/ligi');
}

/** Profilul unui jucător: identitate + statistici pe sezoane. */
export function getJucator(playerId: number): Promise<PaginaJucator> {
  return request<PaginaJucator>(`/v1/jucatori/${playerId}`);
}
