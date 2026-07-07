import { useCallback, useEffect, useState } from 'react';
import { ApiError, getMatchCenter } from '../api/client';
import type { FixtureLive, MeciCentral } from '../api/types';
import { useLiveScore } from './useLiveScore';

interface RezultatMatchCenter {
  date: MeciCentral | null;
  loading: boolean;
  eroare: ApiError | null;
  reincarca: () => void;
  /** Ultimul mesaj WebSocket (scor/minut) — mai proaspat decat snapshot-ul REST cat timp meciul e live. */
  live: FixtureLive | null;
}

/**
 * Detaliul unui meci pentru Match Center: fetch initial + push-uri live pe WebSocket.
 * Cat timp meciul e in desfasurare, re-interogheaza REST-ul la 15s pentru statistici si
 * cronologie (WS transporta doar scor/minut). Polling-ul se opreste cand meciul nu mai e in joc.
 */
export function useMatchCenter(fixtureId: string | undefined): RezultatMatchCenter {
  const id = Number(fixtureId);
  const idValid = Number.isInteger(id) && id > 0;

  const [date, setDate] = useState<MeciCentral | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  const live = useLiveScore(idValid ? id : null);

  const reincarca = useCallback(() => setIncercare((n) => n + 1), []);

  useEffect(() => {
    if (!idValid) {
      setEroare(new ApiError(0, 'Meci invalid', 'Identificatorul meciului nu este valid.'));
      setLoading(false);
      return;
    }
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getMatchCenter(id)
      .then((rezultat) => {
        if (!anulat) setDate(rezultat);
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setDate(null);
          setEroare(e instanceof ApiError ? e : new ApiError(0, 'Eroare neașteptată', String(e)));
        }
      })
      .finally(() => {
        if (!anulat) setLoading(false);
      });
    return () => {
      anulat = true;
    };
  }, [id, idValid, incercare]);

  // Polling pentru statistici + cronologie doar cat timp meciul e in desfasurare.
  const inDesfasurare = date?.inDesfasurare ?? false;
  useEffect(() => {
    if (!idValid || !inDesfasurare) {
      return;
    }
    let anulat = false;
    const interval = setInterval(() => {
      getMatchCenter(id)
        .then((rezultat) => {
          if (!anulat) setDate(rezultat);
        })
        .catch(() => {
          // eroare tranzitorie: pastram datele curente, reincercam la urmatorul tick
        });
      // aliniat cu poll-ul colectorului: evenimentele vin la ~15s, statisticile la ~120s server-side
    }, 15_000);
    return () => {
      anulat = true;
      clearInterval(interval);
    };
  }, [id, idValid, inDesfasurare]);

  return { date, loading, eroare, reincarca, live };
}
