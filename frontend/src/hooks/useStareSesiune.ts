import { useEffect, useState } from 'react';

/**
 * useState reținut în sessionStorage: la „Înapoi" pagina revine unde ai rămas (dată, piață, filtre),
 * iar un tab nou pornește de la valorile implicite.
 */
export function useStareSesiune<T>(cheie: string, initial: T | (() => T)) {
  const [valoare, setValoare] = useState<T>(() => {
    try {
      const salvat = sessionStorage.getItem(`golstat.${cheie}`);
      if (salvat != null) return JSON.parse(salvat) as T;
    } catch {
      // storage indisponibil sau valoare coruptă → implicit
    }
    return typeof initial === 'function' ? (initial as () => T)() : initial;
  });

  useEffect(() => {
    try {
      sessionStorage.setItem(`golstat.${cheie}`, JSON.stringify(valoare));
    } catch {
      // fără storage starea rămâne doar în memorie
    }
  }, [cheie, valoare]);

  return [valoare, setValoare] as const;
}
