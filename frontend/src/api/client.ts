import type { ClasamentDetaliat, MeciCentral, MeciLive, PaginaCompetitie, PaginaEchipa, PaginaJucator, PieteZile, PredictieMeciDto, PrevizualizareMeciDto, Program, ProgramZiGrupat, RezultatCautare, StatisticiLiga } from './types';

const BASE = '/api';

/** Eroare HTTP a API-ului, construita din corpul RFC 7807 (ProblemDetail) cand exista. */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string | null;
  /** Cod stabil pentru logica din UI (ex. `csrf`, `email-neconfirmat`, `parola-slaba`). */
  readonly cod: string | null;
  /** Erorile de validare pe campuri: `{ camp: mesaj }`. */
  readonly erori: Record<string, string> | null;
  /** Secunde pana la o noua incercare (429). */
  readonly retryAfter: number | null;

  constructor(
    status: number,
    title: string,
    detail: string | null,
    extra: { cod?: string | null; erori?: Record<string, string> | null; retryAfter?: number | null } = {},
  ) {
    super(detail ?? title);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.cod = extra.cod ?? null;
    this.erori = extra.erori ?? null;
    this.retryAfter = extra.retryAfter ?? null;
  }
}

interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  cod?: string;
  erori?: Record<string, string>;
  retryAfter?: number;
}

export interface OptiuniCerere {
  method?: 'GET' | 'POST';
  body?: unknown;
  signal?: AbortSignal;
}

const COOKIE_XSRF = 'XSRF-TOKEN';

function citesteTokenXsrf(): string | null {
  const gasit = document.cookie.split('; ').find((c) => c.startsWith(`${COOKIE_XSRF}=`));
  if (!gasit) return null;
  try {
    return decodeURIComponent(gasit.slice(COOKIE_XSRF.length + 1)) || null;
  } catch {
    return null;
  }
}

/** Orice GET primeste cookie-ul XSRF de la server; daca lipseste (prima vizita, dupa logout), il cerem. */
async function asiguraTokenXsrf(): Promise<string | null> {
  const existent = citesteTokenXsrf();
  if (existent) return existent;
  await fetch(`${BASE}/v1/auth/eu`, { credentials: 'same-origin', headers: { Accept: 'application/json' } }).catch(
    () => undefined,
  );
  return citesteTokenXsrf();
}

async function trimite(path: string, { method = 'GET', body, signal }: OptiuniCerere): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') {
    const token = await asiguraTokenXsrf();
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  try {
    return await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'same-origin',
      signal,
    });
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e;
    throw new ApiError(0, 'Conexiune eșuată', 'Serverul nu a putut fi contactat.');
  }
}

async function eroareDin(res: Response): Promise<ApiError> {
  let status = res.status;
  let title = `Eroare ${res.status}`;
  let detail: string | null = null;
  let corp: ProblemDetail = {};
  try {
    corp = (await res.json()) as ProblemDetail;
    if (typeof corp.status === 'number') status = corp.status;
    if (corp.title) title = corp.title;
    if (corp.detail) detail = corp.detail;
  } catch {
    // corp gol sau non-JSON — pastram mesajul generic
  }
  const antetRetry = Number(res.headers.get('Retry-After'));
  return new ApiError(status, title, detail, {
    cod: corp.cod,
    erori: corp.erori,
    retryAfter: corp.retryAfter ?? (Number.isFinite(antetRetry) && antetRetry > 0 ? antetRetry : null),
  });
}

/**
 * Cerere catre API. Pe metodele care modifica trimite tokenul CSRF (double-submit) si, daca serverul
 * il respinge (`cod: csrf`), reincearca o singura data cu un token proaspat. 202/204 → `undefined`.
 */
export async function cerere<T>(path: string, optiuni: OptiuniCerere = {}): Promise<T> {
  let res = await trimite(path, optiuni);
  if (!res.ok) {
    let eroare = await eroareDin(res);
    if (eroare.cod === 'csrf' && optiuni.method && optiuni.method !== 'GET') {
      document.cookie = `${COOKIE_XSRF}=; Max-Age=0; Path=/`;
      res = await trimite(path, optiuni);
      if (res.ok) return corpDin<T>(res);
      eroare = await eroareDin(res);
    }
    throw eroare;
  }
  return corpDin<T>(res);
}

async function corpDin<T>(res: Response): Promise<T> {
  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new ApiError(res.status, 'Răspuns invalid', 'Serverul a trimis un răspuns neașteptat.');
  }
}

function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  return cerere<T>(path, { signal });
}

/** Predictiile meciurilor viitoare ale unei ligi intr-o zi; `data` in format "YYYY-MM-DD". */
export function getPredictiiZi(leagueId: number, data: string): Promise<PredictieMeciDto[]> {
  const params = new URLSearchParams({ leagueId: String(leagueId), data });
  return request<PredictieMeciDto[]>(`/v1/predictii/meciuri?${params.toString()}`);
}

/** Ce meciuri intra in ferestrele de forma: toate competitiile sau doar competitia meciului. */
export type FiltruCompetitie = 'toate' | 'curenta';

/** Previzualizarea completa a unui meci viitor. */
/** `separat` = regula acasă/deplasare (implicit); `toate` = ultimele meciuri indiferent de teren. */
export type FiltruLocatie = 'separat' | 'toate';

export function getPrevizualizare(
  fixtureId: number,
  competitie: FiltruCompetitie = 'toate',
  locatie: FiltruLocatie = 'separat',
): Promise<PrevizualizareMeciDto> {
  const parametri = new URLSearchParams();
  if (competitie === 'curenta') parametri.set('competitie', 'curenta');
  if (locatie === 'toate') parametri.set('locatie', 'toate');
  const query = parametri.toString();
  const sufix = query ? `?${query}` : '';
  return request<PrevizualizareMeciDto>(`/v1/predictii/meciuri/${fixtureId}/previzualizare${sufix}`);
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

/** Clasamentul general / acasă / deplasare cu forma, calculat din meciurile jucate. */
export function getClasament(leagueId: number, sezon: number): Promise<ClasamentDetaliat> {
  return request<ClasamentDetaliat>(`/v1/competitii/${leagueId}/clasament?sezon=${sezon}`);
}

/** Clasamentul de tendințe pe ligi (medii goluri/cornere/faulturi/cartonașe) pentru pagina Statistici. */
export function getStatisticiLigi(): Promise<StatisticiLiga[]> {
  return request<StatisticiLiga[]>('/v1/statistici/ligi');
}

/** Profilul unui jucător: identitate + statistici pe sezoane. */
export function getJucator(playerId: number): Promise<PaginaJucator> {
  return request<PaginaJucator>(`/v1/jucatori/${playerId}`);
}

/** Probabilitățile pe piețe ale meciurilor din următoarele `zile` zile (1–7), grupate pe zi. */
/** Perioada aleasă de utilizator, date ISO „YYYY-MM-DD", inclusiv capetele. */
export interface Perioada {
  de: string;
  pana: string;
}

export function getPieteZile(
  zile = 3,
  competitie: FiltruCompetitie = 'toate',
  perioada?: Perioada,
  locatie: FiltruLocatie = 'separat',
): Promise<PieteZile> {
  const params = new URLSearchParams({ zile: String(zile) });
  if (competitie === 'curenta') params.set('competitie', 'curenta');
  if (locatie === 'toate') params.set('locatie', 'toate');
  if (perioada) {
    params.set('de', perioada.de);
    params.set('pana', perioada.pana);
  }
  return request<PieteZile>(`/v1/piete/zile?${params.toString()}`);
}
