import { act, renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { EVENIMENT_FAVORITE } from '../lib/favorite';
import { echipa } from '../test/factories';
import { useFavorite } from './useFavorite';

describe('useFavorite', () => {
  test('identitate stabila: re-randarea fara schimbari NU produce alt obiect/Set', () => {
    const { result, rerender } = renderHook(() => useFavorite());
    const inainte = result.current;
    rerender();
    // regresie: inainte hook-ul construia un obiect si un Set noi la fiecare randare,
    // invalidand memo-urile consumatorilor (ex. filtrarea din MeciuriPage)
    expect(result.current).toBe(inainte);
    expect(result.current.iduri).toBe(inainte.iduri);
  });

  test('comuta adauga si scoate echipa, iar predicatul reflecta starea', () => {
    const { result } = renderHook(() => useFavorite());
    expect(result.current.este(33)).toBe(false);

    act(() => result.current.comuta(echipa({ id: 33 })));
    expect(result.current.este(33)).toBe(true);
    expect(result.current.echipe.map((e) => e.id)).toContain(33);

    act(() => result.current.comuta(echipa({ id: 33 })));
    expect(result.current.este(33)).toBe(false);
    expect(result.current.echipe).toHaveLength(0);
  });

  test('predicatul intoarce false pentru id null/undefined', () => {
    const { result } = renderHook(() => useFavorite());
    expect(result.current.este(null)).toBe(false);
    expect(result.current.este(undefined)).toBe(false);
  });

  test('o scriere din alta parte a aplicatiei + evenimentul intern reimprospateaza lista', () => {
    const { result } = renderHook(() => useFavorite());
    act(() => {
      localStorage.setItem(
        'golstat-echipe-favorite',
        JSON.stringify({ 40: { id: 40, nume: 'Liverpool', logo: null } }),
      );
      window.dispatchEvent(new Event(EVENIMENT_FAVORITE));
    });
    expect(result.current.este(40)).toBe(true);
  });
});
