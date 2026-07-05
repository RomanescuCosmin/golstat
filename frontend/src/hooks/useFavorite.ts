import { useEffect, useState } from 'react';
import type { EchipaDto } from '../api/types';
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

  const iduri = new Set(echipe.map((e) => e.id));

  return {
    echipe,
    iduri,
    este: (teamId: number | null | undefined) => (teamId != null ? iduri.has(teamId) : false),
    comuta: (echipa: EchipaDto) => comutaFavorita(echipa),
    // varianta necached (utila in afara React, dar pastram simetria API-ului)
    esteStatic: esteFavorita,
  };
}
