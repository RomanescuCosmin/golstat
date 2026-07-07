import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  comutaFavorita,
  echipeFavorite,
  EVENIMENT_FAVORITE,
  esteFavorita,
  type EchipaFavorita,
} from '../lib/favorite';

/**
 * Expune echipele favorite (din localStorage) si reactioneaza la schimbari — atat din tab-ul curent
 * (eveniment intern) cat si din alte taburi (`storage`). Returneaza lista, un set de id-uri, un
 * predicat si comutatorul.
 */
export function useFavorite() {
  const [echipe, setEchipe] = useState<EchipaFavorita[]>(() => echipeFavorite());

  useEffect(() => {
    const reincarca = () => setEchipe(echipeFavorite());
    window.addEventListener(EVENIMENT_FAVORITE, reincarca);
    window.addEventListener('storage', reincarca);
    return () => {
      window.removeEventListener(EVENIMENT_FAVORITE, reincarca);
      window.removeEventListener('storage', reincarca);
    };
  }, []);

  // Identitate stabila intre randari: rezultatul intra in dependintele memo-urilor consumatorilor
  // (ex. filtrarea din MeciuriPage), care altfel ar recalcula la fiecare randare.
  const iduri = useMemo(() => new Set(echipe.map((e) => e.id)), [echipe]);
  const este = useCallback(
    (teamId: number | null | undefined) => (teamId != null ? iduri.has(teamId) : false),
    [iduri],
  );

  return useMemo(
    () => ({
      echipe,
      iduri,
      este,
      comuta: comutaFavorita,
      // varianta necached (utila in afara React, dar pastram simetria API-ului)
      esteStatic: esteFavorita,
    }),
    [echipe, iduri, este],
  );
}
