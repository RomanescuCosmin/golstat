import { vi } from 'vitest';
import type { LiveHandler } from '../live';

/**
 * Mock manual pentru singleton-ul STOMP/SockJS: niciun socket real in jsdom.
 * Testele care randeaza componente live fac `vi.mock('.../api/live')` si pot importa
 * `emiteLive` direct din acest fisier ca sa simuleze push-uri WebSocket (in `act()`).
 */
const abonati = new Map<number, Set<LiveHandler>>();

export const connectLive = vi.fn();

export const subscribeFixture = vi.fn((fixtureId: number, cb: LiveHandler) => {
  let set = abonati.get(fixtureId);
  if (!set) {
    set = new Set();
    abonati.set(fixtureId, set);
  }
  set.add(cb);
  return () => {
    set?.delete(cb);
    if (set?.size === 0) {
      abonati.delete(fixtureId);
    }
  };
});

export const disconnectLive = vi.fn(() => {
  abonati.clear();
});

/** Impinge un payload fake tuturor abonatilor unui meci (apeleaza in `act()`). */
export function emiteLive(fixtureId: number, payload: unknown): void {
  abonati.get(fixtureId)?.forEach((h) => h(payload));
}

/** Cati ascultatori are un meci (verifica abonarea/dezabonarea). */
export function numarAbonati(fixtureId: number): number {
  return abonati.get(fixtureId)?.size ?? 0;
}

export function resetLiveMock(): void {
  abonati.clear();
  connectLive.mockClear();
  subscribeFixture.mockClear();
  disconnectLive.mockClear();
}
