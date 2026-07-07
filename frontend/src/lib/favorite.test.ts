import { describe, expect, test, vi } from 'vitest';
import { echipa } from '../test/factories';
import { EVENIMENT_FAVORITE, comutaFavorita, echipeFavorite, esteFavorita } from './favorite';

describe('favorite (localStorage)', () => {
  test('fara nimic salvat: lista goala si predicat false', () => {
    expect(echipeFavorite()).toEqual([]);
    expect(esteFavorita(33)).toBe(false);
    expect(esteFavorita(null)).toBe(false);
    expect(esteFavorita(undefined)).toBe(false);
  });

  test('comutaFavorita: roundtrip adaugare + eliminare', () => {
    comutaFavorita(echipa({ id: 33, nume: 'Manchester United' }));
    expect(esteFavorita(33)).toBe(true);
    expect(echipeFavorite()).toEqual([{ id: 33, nume: 'Manchester United', logo: null }]);

    comutaFavorita(echipa({ id: 33 }));
    expect(esteFavorita(33)).toBe(false);
    expect(echipeFavorite()).toEqual([]);
  });

  test('orice scriere emite evenimentul intern (hook-urile din acelasi tab asculta)', () => {
    const spy = vi.fn();
    window.addEventListener(EVENIMENT_FAVORITE, spy);
    comutaFavorita(echipa({ id: 40 }));
    expect(spy).toHaveBeenCalledTimes(1);
    window.removeEventListener(EVENIMENT_FAVORITE, spy);
  });

  test('JSON corupt in storage: nu arunca, se comporta ca lista goala', () => {
    localStorage.setItem('golstat-echipe-favorite', '{corupt');
    expect(echipeFavorite()).toEqual([]);
    expect(esteFavorita(33)).toBe(false);
    // iar o comutare porneste curat peste valoarea corupta
    comutaFavorita(echipa({ id: 33 }));
    expect(esteFavorita(33)).toBe(true);
  });
});
