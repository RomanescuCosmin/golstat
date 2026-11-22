import { beforeEach, describe, expect, test } from 'vitest';
import { comutaMeciFavorit, meciuriFavorite, type MeciFavorit } from './meciuriFavorite';

function meci(fixtureId: number, kickoff: string): MeciFavorit {
  return {
    fixtureId,
    kickoff,
    gazde: { id: 1, nume: 'A', logo: null },
    oaspeti: { id: 2, nume: 'B', logo: null },
    leagueId: 39,
    ligaNume: 'Premier League',
  };
}

const ACUM = Date.parse('2026-11-21T12:00:00Z');

describe('meciuriFavorite', () => {
  beforeEach(() => localStorage.clear());

  test('comuta adauga si scoate; lista e ordonata dupa ora de start', () => {
    comutaMeciFavorit(meci(2, '2026-11-21T19:00:00Z'));
    comutaMeciFavorit(meci(1, '2026-11-21T15:00:00Z'));
    expect(meciuriFavorite(ACUM).map((m) => m.fixtureId)).toEqual([1, 2]);

    comutaMeciFavorit(meci(1, '2026-11-21T15:00:00Z'));
    expect(meciuriFavorite(ACUM).map((m) => m.fixtureId)).toEqual([2]);
  });

  test('meciurile mai vechi de 7 zile dispar din favorite', () => {
    comutaMeciFavorit(meci(1, '2026-11-10T15:00:00Z'));
    comutaMeciFavorit(meci(2, '2026-11-18T15:00:00Z'));
    expect(meciuriFavorite(ACUM).map((m) => m.fixtureId)).toEqual([2]);
    expect(Object.keys(JSON.parse(localStorage.getItem('golstat-meciuri-favorite')!))).toEqual(['2']);
  });

  test('storage corupt → lista goala, fara crash', () => {
    localStorage.setItem('golstat-meciuri-favorite', '{nu e json');
    expect(meciuriFavorite(ACUM)).toEqual([]);
  });
});
