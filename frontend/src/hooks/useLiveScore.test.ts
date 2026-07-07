import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { emiteLive, numarAbonati, resetLiveMock } from '../api/__mocks__/live';
import { fixtureLive } from '../test/factories';
import { esteInPlay, minutLive, useLiveScore, useLiveScores } from './useLiveScore';

vi.mock('../api/live');

beforeEach(() => resetLiveMock());

describe('esteInPlay', () => {
  test.each(['1H', 'HT', '2H', 'ET', 'P'])('status %s → in desfasurare', (status) => {
    expect(esteInPlay(fixtureLive({ statusShort: status }))).toBe(true);
  });

  test.each(['NS', 'FT', 'AET', 'PEN', 'PST'])('status %s → nu e in desfasurare', (status) => {
    expect(esteInPlay(fixtureLive({ statusShort: status }))).toBe(false);
  });

  test('null / status lipsa → false', () => {
    expect(esteInPlay(null)).toBe(false);
    expect(esteInPlay(undefined)).toBe(false);
    expect(esteInPlay(fixtureLive({ statusShort: null }))).toBe(false);
  });
});

describe('minutLive', () => {
  test('minutul cu apostrof; "Pauză" la HT; "LIVE" fara minut', () => {
    expect(minutLive(fixtureLive({ statusShort: '2H', statusElapsed: 67 }))).toBe("67'");
    expect(minutLive(fixtureLive({ statusShort: 'HT', statusElapsed: 45 }))).toBe('Pauză');
    expect(minutLive(fixtureLive({ statusShort: '1H', statusElapsed: null }))).toBe('LIVE');
  });
});

describe('useLiveScore', () => {
  test('null pana la primul mesaj, apoi ultimul payload valid', () => {
    const { result } = renderHook(() => useLiveScore(2001));
    expect(result.current).toBeNull();

    act(() => emiteLive(2001, fixtureLive({ id: 2001, goalsHome: 1 })));
    expect(result.current?.goalsHome).toBe(1);

    act(() => emiteLive(2001, fixtureLive({ id: 2001, goalsHome: 2 })));
    expect(result.current?.goalsHome).toBe(2);
  });

  test('payload-urile care nu sunt FixtureLive sunt ignorate', () => {
    const { result } = renderHook(() => useLiveScore(2001));
    act(() => emiteLive(2001, 'text-neasteptat'));
    act(() => emiteLive(2001, { fara: 'id' }));
    expect(result.current).toBeNull();
  });

  test('schimbarea meciului reseteaza scorul si muta abonarea', () => {
    const { result, rerender } = renderHook(({ id }) => useLiveScore(id), {
      initialProps: { id: 2001 },
    });
    act(() => emiteLive(2001, fixtureLive({ id: 2001 })));
    expect(result.current).not.toBeNull();

    rerender({ id: 2002 });
    expect(result.current).toBeNull();
    expect(numarAbonati(2001)).toBe(0);
    expect(numarAbonati(2002)).toBe(1);
  });

  test('unmount: dezabonare completa', () => {
    const { unmount } = renderHook(() => useLiveScore(2001));
    expect(numarAbonati(2001)).toBe(1);
    unmount();
    expect(numarAbonati(2001)).toBe(0);
  });

  test('fixtureId null: nicio abonare', () => {
    renderHook(() => useLiveScore(null));
    expect(numarAbonati(2001)).toBe(0);
  });
});

describe('useLiveScores', () => {
  test('indexeaza ultimul mesaj pe fiecare fixture', () => {
    const { result } = renderHook(() => useLiveScores([1, 2]));
    act(() => emiteLive(1, fixtureLive({ id: 1, goalsHome: 1 })));
    act(() => emiteLive(2, fixtureLive({ id: 2, goalsHome: 3 })));
    expect(result.current[1]?.goalsHome).toBe(1);
    expect(result.current[2]?.goalsHome).toBe(3);
  });

  test('schimbarea listei pastreaza scorurile id-urilor ramase (merge, nu reset)', () => {
    const { result, rerender } = renderHook(({ ids }) => useLiveScores(ids), {
      initialProps: { ids: [1, 2] },
    });
    act(() => emiteLive(1, fixtureLive({ id: 1, goalsHome: 2 })));

    rerender({ ids: [1, 3] });
    // scorul meciului 1 nu clipeste la re-abonare, meciul 2 dispare
    expect(result.current[1]?.goalsHome).toBe(2);
    expect(result.current[2]).toBeUndefined();
    expect(numarAbonati(2)).toBe(0);
    expect(numarAbonati(3)).toBe(1);
  });

  test('lista goala: fara abonari si rezultat gol', () => {
    const { result } = renderHook(() => useLiveScores([]));
    expect(result.current).toEqual({});
    expect(numarAbonati(1)).toBe(0);
  });
});
