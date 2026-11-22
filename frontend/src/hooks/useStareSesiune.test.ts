import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { useStareSesiune } from './useStareSesiune';

describe('useStareSesiune', () => {
  afterEach(() => vi.restoreAllMocks());

  test('fara nimic salvat porneste de la valoarea initiala (si accepta initializator lenes)', () => {
    expect(renderHook(() => useStareSesiune('a', 5)).result.current[0]).toBe(5);
    const init = vi.fn(() => 'x');
    expect(renderHook(() => useStareSesiune('b', init)).result.current[0]).toBe('x');
    expect(init).toHaveBeenCalledTimes(1);
  });

  test('scrie in golstat.<cheie> si o recitește la re-montare („Înapoi")', () => {
    const { result, unmount } = renderHook(() => useStareSesiune('piete.prag', 30));
    act(() => result.current[1](65));
    expect(sessionStorage.getItem('golstat.piete.prag')).toBe('65');
    unmount();

    const dinNou = renderHook(() => useStareSesiune('piete.prag', 30));
    expect(dinNou.result.current[0]).toBe(65);
  });

  test('pastreaza structuri (liste, obiecte) intre montari', () => {
    const { result, unmount } = renderHook(() => useStareSesiune<number[]>('ligi', []));
    act(() => result.current[1]([39, 140]));
    unmount();
    expect(renderHook(() => useStareSesiune<number[]>('ligi', [])).result.current[0]).toEqual([39, 140]);
  });

  test('cheile sunt independente', () => {
    sessionStorage.setItem('golstat.unu', '1');
    expect(renderHook(() => useStareSesiune('doi', 2)).result.current[0]).toBe(2);
  });

  test('valoare corupta in storage → valoarea initiala, fara crash', () => {
    sessionStorage.setItem('golstat.stricat', '{nu e json');
    expect(renderHook(() => useStareSesiune('stricat', 'implicit')).result.current[0]).toBe('implicit');
  });

  test('storage indisponibil (arunca) → merge doar in memorie', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocat');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocat');
    });
    const { result } = renderHook(() => useStareSesiune('x', 1));
    expect(result.current[0]).toBe(1);
    act(() => result.current[1](2));
    expect(result.current[0]).toBe(2);
  });
});
