import type { EchipaDto } from '../api/types';

/** Un meci favorit salvat local: destul cat sa-l afisam pe pagina Favorite fara alt fetch. */
export interface MeciFavorit {
  fixtureId: number;
  /** ISO-8601. */
  kickoff: string;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  leagueId: number | null;
  ligaNume: string | null;
}

const CHEIE = 'golstat-meciuri-favorite';
export const EVENIMENT_MECIURI_FAVORITE = 'golstat-meciuri-favorite-schimbat';

/** Meciurile mai vechi de atat dispar singure din favorite (lista nu creste la nesfarsit). */
const PASTRARE_MS = 7 * 24 * 60 * 60 * 1000;

function citeste(): Record<number, MeciFavorit> {
  try {
    const brut = localStorage.getItem(CHEIE);
    const map = brut ? (JSON.parse(brut) as Record<number, MeciFavorit>) : {};
    return map && typeof map === 'object' ? map : {};
  } catch {
    return {};
  }
}

function scrie(map: Record<number, MeciFavorit>) {
  try {
    localStorage.setItem(CHEIE, JSON.stringify(map));
  } catch {
    // localStorage indisponibil (mod privat) — favoritele nu persista, dar app-ul merge.
  }
  window.dispatchEvent(new Event(EVENIMENT_MECIURI_FAVORITE));
}

/** Meciurile favorite, in ordinea startului; cele expirate (> 7 zile) sunt sterse la citire. */
export function meciuriFavorite(acum: number = Date.now()): MeciFavorit[] {
  const map = citeste();
  const valide = Object.values(map).filter((m) => {
    const t = Date.parse(m.kickoff);
    return Number.isNaN(t) || acum - t < PASTRARE_MS;
  });
  if (valide.length !== Object.keys(map).length) {
    try {
      localStorage.setItem(CHEIE, JSON.stringify(Object.fromEntries(valide.map((m) => [m.fixtureId, m]))));
    } catch {
      /* doar in memorie */
    }
  }
  return valide.sort((a, b) => Date.parse(a.kickoff) - Date.parse(b.kickoff));
}

export function comutaMeciFavorit(meci: MeciFavorit) {
  const map = citeste();
  if (map[meci.fixtureId]) {
    delete map[meci.fixtureId];
  } else {
    map[meci.fixtureId] = meci;
  }
  scrie(map);
}
