import type { FiltruCompetitie } from '../api/client';

const CHEIE = 'golstat.filtruCompetitie';

/** Filtrul de competiție e reținut între pagini; storage-ul poate lipsi (mod privat) — atunci „toate". */
export function citesteFiltruCompetitie(): FiltruCompetitie {
  try {
    return localStorage.getItem(CHEIE) === 'curenta' ? 'curenta' : 'toate';
  } catch {
    return 'toate';
  }
}

export function salveazaFiltruCompetitie(valoare: FiltruCompetitie) {
  try {
    localStorage.setItem(CHEIE, valoare);
  } catch {
    // fără storage filtrul rămâne doar pentru sesiunea curentă
  }
}
