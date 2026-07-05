import { useEffect, useState } from 'react';
import { subscribeFixture } from '../api/live';
import type { FixtureLive } from '../api/types';

const IN_PLAY = new Set(['1H', 'HT', '2H', 'ET', 'P']);

/** True daca mesajul live descrie un meci in desfasurare (repriza 1/pauza/repriza 2/prelungiri/penalty-uri). */
export function esteInPlay(live: FixtureLive | null | undefined): live is FixtureLive {
  return live != null && live.statusShort != null && IN_PLAY.has(live.statusShort);
}

/** Eticheta de minut pentru banda LIVE: "67'" sau "Pauză" la HT. */
export function minutLive(live: FixtureLive): string {
  if (live.statusShort === 'HT') {
    return 'Pauză';
  }
  return live.statusElapsed != null ? `${live.statusElapsed}'` : 'LIVE';
}

function esteFixtureLive(payload: unknown): payload is FixtureLive {
  return (
    typeof payload === 'object' &&
    payload !== null &&
    typeof (payload as { id?: unknown }).id === 'number'
  );
}

/**
 * Se aboneaza la `/topic/live/{fixtureId}` si intoarce ultimul {@link FixtureLive} primit
 * (sau `null` cat timp nu a venit nimic). Conexiunea STOMP e partajata intre toate hook-urile;
 * abonarea e curatata la unmount.
 */
export function useLiveScore(fixtureId: number | null | undefined): FixtureLive | null {
  const [live, setLive] = useState<FixtureLive | null>(null);

  useEffect(() => {
    setLive(null);
    if (!fixtureId) {
      return;
    }
    return subscribeFixture(fixtureId, (payload) => {
      if (esteFixtureLive(payload)) {
        setLive(payload);
      }
    });
  }, [fixtureId]);

  return live;
}

/**
 * Varianta pentru o lista de meciuri: intoarce ultimul {@link FixtureLive} primit pe fiecare
 * fixture, indexat dupa id. Meciurile fara niciun mesaj live lipsesc din rezultat.
 */
export function useLiveScores(fixtureIds: number[]): Record<number, FixtureLive> {
  const [scoruri, setScoruri] = useState<Record<number, FixtureLive>>({});
  // Cheie stabila: evita re-abonarea cand pagina re-randeaza cu aceeasi lista de id-uri.
  const cheie = fixtureIds.join(',');

  useEffect(() => {
    const ids = cheie ? cheie.split(',').map(Number) : [];
    // Merge, nu reset: pastram scorurile deja primite pentru id-urile inca prezente,
    // altfel fiecare schimbare a listei (ex. polling) ar face scorul sa clipeasca.
    setScoruri((prev) => {
      const pastrate: Record<number, FixtureLive> = {};
      for (const id of ids) {
        if (prev[id] != null) {
          pastrate[id] = prev[id];
        }
      }
      return pastrate;
    });
    if (ids.length === 0) {
      return;
    }
    const dezabonari = ids.map((id) => {
      return subscribeFixture(id, (payload) => {
        if (esteFixtureLive(payload)) {
          setScoruri((prev) => ({ ...prev, [id]: payload }));
        }
      });
    });
    return () => {
      dezabonari.forEach((dezaboneaza) => dezaboneaza());
    };
  }, [cheie]);

  return scoruri;
}
