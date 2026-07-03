/** Ligile disponibile in aplicatie (id-uri API-Football). */
export interface Liga {
  id: number;
  nume: string;
  regiune: string;
}

export const LIGI: Liga[] = [
  { id: 1, nume: 'Campionatul Mondial 2026', regiune: 'Internațional' },
  { id: 39, nume: 'Premier League', regiune: 'Anglia' },
];

export function numeLiga(id: number): string {
  return LIGI.find((l) => l.id === id)?.nume ?? `Liga #${id}`;
}
