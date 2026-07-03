import type { EchipaDto } from '../api/types';

/** Numele echipei pentru afisare; fallback pe id cand echipa nu e in DB. */
export function numeEchipa(echipa: EchipaDto): string {
  return echipa.nume ?? `Echipa #${echipa.id}`;
}
