/** Ligile disponibile in aplicatie (id-uri API-Football). */
export interface Liga {
  id: number;
  nume: string;
  regiune: string;
}

export const LIGI: Liga[] = [
  { id: 1, nume: 'Campionatul Mondial 2026', regiune: 'Internațional' },
  { id: 2, nume: 'UEFA Champions League', regiune: 'Europa' },
  { id: 3, nume: 'UEFA Europa League', regiune: 'Europa' },
  { id: 848, nume: 'UEFA Conference League', regiune: 'Europa' },
  { id: 39, nume: 'Premier League', regiune: 'Anglia' },
  { id: 40, nume: 'Championship', regiune: 'Anglia' },
  { id: 41, nume: 'League One', regiune: 'Anglia' },
  { id: 140, nume: 'La Liga', regiune: 'Spania' },
  { id: 141, nume: 'La Liga 2', regiune: 'Spania' },
  { id: 135, nume: 'Serie A', regiune: 'Italia' },
  { id: 136, nume: 'Serie B', regiune: 'Italia' },
  { id: 78, nume: 'Bundesliga', regiune: 'Germania' },
  { id: 79, nume: '2. Bundesliga', regiune: 'Germania' },
  { id: 61, nume: 'Ligue 1', regiune: 'Franța' },
  { id: 62, nume: 'Ligue 2', regiune: 'Franța' },
  { id: 88, nume: 'Eredivisie', regiune: 'Olanda' },
  { id: 89, nume: 'Eerste Divisie', regiune: 'Olanda' },
  { id: 144, nume: 'Jupiler Pro League', regiune: 'Belgia' },
  { id: 94, nume: 'Primeira Liga', regiune: 'Portugalia' },
  { id: 283, nume: 'Liga I', regiune: 'România' },
  { id: 203, nume: 'Süper Lig', regiune: 'Turcia' },
  { id: 113, nume: 'Allsvenskan', regiune: 'Suedia' },
  { id: 119, nume: 'Superliga', regiune: 'Danemarca' },
  { id: 103, nume: 'Eliteserien', regiune: 'Norvegia' },
  { id: 197, nume: 'Super League', regiune: 'Grecia' },
  { id: 667, nume: 'Amicale cluburi', regiune: 'Internațional' },
];

/** „Competiții populare" din rail-ul drept: top 5 campionate + cupele europene + Cupa Mondiala. */
export const LIGI_POPULARE: number[] = [39, 140, 135, 78, 61, 2, 3, 848, 1];

export function numeLiga(id: number): string {
  return LIGI.find((l) => l.id === id)?.nume ?? `Liga #${id}`;
}

/** Id-ul ligii Campionatul Mondial la API-Football (are doar placeholder gri → logo desenat de noi). */
export const ID_CUPA_MONDIALA = 1;

/**
 * Ligile pentru care API-Football serveste un PLACEHOLDER gri (scut gol) in loc de logo real.
 * Le tratam ca "fara logo" al sursei → afisam un logo propriu. Ex.: Campionatul Mondial (1).
 */
const LOGO_PLACEHOLDER = new Set<number>([ID_CUPA_MONDIALA]);

/** Logoul unei ligi de pe CDN-ul API-Football; {@code null} cand sursa are doar placeholder. */
export function logoLiga(id: number): string | null {
  if (LOGO_PLACEHOLDER.has(id)) {
    return null;
  }
  return `https://media.api-sports.io/football/leagues/${id}.png`;
}

/** Extrage id-ul ligii dintr-un URL de logo API-Football (`.../leagues/{id}.png`). */
export function idLigaDinLogo(url: string): number | null {
  const m = url.match(/\/leagues\/(\d+)\.png/);
  return m ? Number(m[1]) : null;
}

/** Id-ul efectiv al ligii: dat direct sau extras din URL-ul logoului. */
export function idLigaEfectiv(id: number | null | undefined, url?: string | null): number | null {
  return id ?? (url ? idLigaDinLogo(url) : null);
}

/** Un logo de ligă e "placeholder" daca id-ul (dat direct sau extras din URL) e in lista de placeholder. */
export function esteLogoPlaceholder(id: number | null | undefined, url?: string | null): boolean {
  const efectiv = idLigaEfectiv(id, url);
  return efectiv != null && LOGO_PLACEHOLDER.has(efectiv);
}
