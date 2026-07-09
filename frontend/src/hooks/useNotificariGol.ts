import { useCallback, useEffect, useRef, useState } from 'react';
import { getLive } from '../api/client';
import type { EchipaDto, MeciLive } from '../api/types';
import { sunetGol } from '../lib/sunet';
import { useFavorite } from './useFavorite';

/** O notificare de gol pentru un meci care implica o echipa favorita. */
export interface NotificareGol {
  /** Cheie unica per gol (fixture + scor + cine a inscris) — evita duplicate la re-polling. */
  cheie: string;
  fixtureId: number;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  golGazde: number;
  golOaspeti: number;
  minut: number | null;
  /** true = au inscris gazdele (GOOL la gazde); false = oaspetii. */
  marcatorAcasa: boolean;
  ligaNume: string | null;
}

const INTERVAL_MS = 15000;
const DURATA_MS = 8000;
const MAX_VIZIBILE = 4;

/**
 * Urmareste global meciurile live ({@link getLive}) si emite o {@link NotificareGol} de fiecare data
 * cand scorul creste intr-un meci ce implica o echipa favorita — la orice gol (si cand inscrie, si
 * cand primeste), cu marcatorul evidentiat. La montare stabileste doar linia de baza (nu anunta
 * golurile deja marcate). Suna o data per lot de goluri noi.
 */
export function useNotificariGol() {
  const { iduri } = useFavorite();
  const [notificari, setNotificari] = useState<NotificareGol[]>([]);
  const scoruriPrec = useRef<Map<number, { h: number; a: number }>>(new Map());
  // iduri se schimba ca referinta la fiecare comutare; il tinem intr-un ref ca sa nu repornim polling-ul.
  const iduriRef = useRef(iduri);
  iduriRef.current = iduri;

  const inchide = useCallback((cheie: string) => {
    setNotificari((prev) => prev.filter((n) => n.cheie !== cheie));
  }, []);

  useEffect(() => {
    let anulat = false;

    async function verifica() {
      let live: MeciLive[];
      try {
        live = await getLive();
      } catch {
        return; // backend picat / eroare de retea → reincercam la urmatorul tick
      }
      if (anulat) return;

      const fav = iduriRef.current;
      const prec = scoruriPrec.current;
      const vazute = new Set<number>();
      const noi: NotificareGol[] = [];

      for (const m of live) {
        const h = m.golGazde ?? 0;
        const a = m.golOaspeti ?? 0;
        vazute.add(m.fixtureId);
        const anterior = prec.get(m.fixtureId);
        prec.set(m.fixtureId, { h, a });

        const eFavorit = fav.has(m.gazde.id ?? -1) || fav.has(m.oaspeti.id ?? -1);
        if (!eFavorit || !anterior) continue; // fara favorita sau prim contact cu meciul → doar linia de baza

        if (h > anterior.h) noi.push(construieste(m, h, a, true));
        if (a > anterior.a) noi.push(construieste(m, h, a, false));
      }

      // uita meciurile care nu mai sunt live (altfel re-anuntam la o eventuala reintrare in play)
      for (const id of [...prec.keys()]) {
        if (!vazute.has(id)) prec.delete(id);
      }

      if (noi.length > 0 && !anulat) {
        sunetGol();
        setNotificari((prev) => [...noi, ...prev].slice(0, MAX_VIZIBILE));
        for (const n of noi) {
          setTimeout(() => {
            if (!anulat) inchide(n.cheie);
          }, DURATA_MS);
        }
      }
    }

    void verifica();
    const t = setInterval(() => void verifica(), INTERVAL_MS);
    return () => {
      anulat = true;
      clearInterval(t);
    };
  }, [inchide]);

  return { notificari, inchide };
}

function construieste(m: MeciLive, h: number, a: number, marcatorAcasa: boolean): NotificareGol {
  return {
    cheie: `${m.fixtureId}:${h}-${a}:${marcatorAcasa ? 'h' : 'a'}`,
    fixtureId: m.fixtureId,
    gazde: m.gazde,
    oaspeti: m.oaspeti,
    golGazde: h,
    golOaspeti: a,
    minut: m.minut,
    marcatorAcasa,
    ligaNume: m.ligaNume,
  };
}
