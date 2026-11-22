import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  comutaMeciFavorit,
  EVENIMENT_MECIURI_FAVORITE,
  meciuriFavorite,
  type MeciFavorit,
} from '../lib/meciuriFavorite';

/** Meciurile favorite (localStorage), sincronizate intre componente si intre taburi. */
export function useMeciuriFavorite() {
  const [meciuri, setMeciuri] = useState<MeciFavorit[]>(() => meciuriFavorite());

  useEffect(() => {
    const reincarca = () => setMeciuri(meciuriFavorite());
    window.addEventListener(EVENIMENT_MECIURI_FAVORITE, reincarca);
    window.addEventListener('storage', reincarca);
    return () => {
      window.removeEventListener(EVENIMENT_MECIURI_FAVORITE, reincarca);
      window.removeEventListener('storage', reincarca);
    };
  }, []);

  const iduri = useMemo(() => new Set(meciuri.map((m) => m.fixtureId)), [meciuri]);
  const este = useCallback((fixtureId: number) => iduri.has(fixtureId), [iduri]);

  return useMemo(() => ({ meciuri, iduri, este, comuta: comutaMeciFavorit }), [meciuri, iduri, este]);
}
