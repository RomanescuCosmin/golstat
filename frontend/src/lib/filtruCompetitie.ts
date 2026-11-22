import type { FiltruCompetitie, FiltruLocatie } from '../api/client';

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

const CHEIE_LOCATIE = 'golstat.filtruLocatie';

/** „Acasă / Deplasare" sau „Toate meciurile", reținut între meciuri; implicit regula acasă/deplasare. */
export function citesteFiltruLocatie(): FiltruLocatie {
  try {
    return localStorage.getItem(CHEIE_LOCATIE) === 'toate' ? 'toate' : 'separat';
  } catch {
    return 'separat';
  }
}

export function salveazaFiltruLocatie(valoare: FiltruLocatie) {
  try {
    localStorage.setItem(CHEIE_LOCATIE, valoare);
  } catch {
    // fără storage filtrul rămâne doar pentru sesiunea curentă
  }
}
