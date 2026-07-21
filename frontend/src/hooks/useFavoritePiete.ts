import { useCallback, useState } from 'react';

const CHEIE = 'golstat:piete:favorite';

/** Citire tolerantă: `localStorage` poate lipsi (SSR) sau conține gunoi de la o versiune veche. */
function citeste(): number[] {
  try {
    const brut = window.localStorage.getItem(CHEIE);
    if (!brut) return [];
    const valoare: unknown = JSON.parse(brut);
    return Array.isArray(valoare) ? valoare.filter((x): x is number => typeof x === 'number') : [];
  } catch {
    return [];
  }
}

/**
 * Meciurile marcate cu steluță, ținute în `localStorage` — nu există încă în API, iar o listă
 * pierdută la fiecare reîncărcare n-ar merita butonul. Eșecul de scriere (mod privat, cotă plină)
 * nu are voie să rupă pagina: marcajul rămâne valabil în sesiunea curentă.
 */
export function useFavoritePiete() {
  const [favorite, setFavorite] = useState<number[]>(citeste);

  const comuta = useCallback((fixtureId: number) => {
    setFavorite((curente) => {
      const urmatoare = curente.includes(fixtureId)
        ? curente.filter((id) => id !== fixtureId)
        : [...curente, fixtureId];
      try {
        window.localStorage.setItem(CHEIE, JSON.stringify(urmatoare));
      } catch {
        /* păstrăm doar în memorie */
      }
      return urmatoare;
    });
  }, []);

  return { favorite, comuta };
}
