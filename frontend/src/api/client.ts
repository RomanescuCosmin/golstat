import type { PredictieMeciDto, PrevizualizareMeciDto } from './types';

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

async function request<T>(path: string): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, { headers: { Accept: 'application/json' } });
  } catch {
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
