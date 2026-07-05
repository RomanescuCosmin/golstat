import type { EchipaDto } from '../api/types';

/** O echipa favorita salvata local (destul cat s-o afisam in rail fara alt fetch). */
export interface EchipaFavorita {
  id: number;
  nume: string | null;
  logo: string | null;
}

const CHEIE = 'golstat-echipe-favorite';
const EVENIMENT = 'golstat-favorite-schimbat';

function citeste(): Record<number, EchipaFavorita> {
  try {
    const brut = localStorage.getItem(CHEIE);
    return brut ? (JSON.parse(brut) as Record<number, EchipaFavorita>) : {};
  } catch {
    return {};
  }
}

function scrie(map: Record<number, EchipaFavorita>) {
  try {
    localStorage.setItem(CHEIE, JSON.stringify(map));
  } catch {
    // localStorage indisponibil (mod privat) — favoritele nu persista, dar app-ul merge.
  }
  // notifica toate hook-urile din tab-ul curent (storage-event nu se declanseaza pe acelasi tab)
  window.dispatchEvent(new Event(EVENIMENT));
}

/** Toate echipele favorite, cele mai recent adaugate reflectate la re-citire. */
export function echipeFavorite(): EchipaFavorita[] {
  return Object.values(citeste());
}

export function esteFavorita(teamId: number | null | undefined): boolean {
  if (teamId == null) return false;
  return teamId in citeste();
}

/** Comuta o echipa in/din favorite; `echipa` trebuie sa aiba un id valid. */
export function comutaFavorita(echipa: EchipaDto) {
  if (echipa.id == null) return;
  const map = citeste();
  if (map[echipa.id]) {
    delete map[echipa.id];
  } else {
    map[echipa.id] = { id: echipa.id, nume: echipa.nume, logo: echipa.logo };
  }
  scrie(map);
}

/** Numele evenimentului intern emis la orice schimbare (pentru hook-uri). */
export const EVENIMENT_FAVORITE = EVENIMENT;
