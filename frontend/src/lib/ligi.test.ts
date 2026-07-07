import { describe, expect, test } from 'vitest';
import { numeEchipa } from './echipa';
import {
  ID_CUPA_MONDIALA,
  esteLogoPlaceholder,
  idLigaDinLogo,
  idLigaEfectiv,
  logoLiga,
  numeLiga,
} from './ligi';

describe('numeLiga', () => {
  test('liga cunoscuta: numele din catalog', () => {
    expect(numeLiga(39)).toBe('Premier League');
    expect(numeLiga(283)).toBe('Liga I');
  });

  test('liga necunoscuta: fallback "Liga #id"', () => {
    expect(numeLiga(999)).toBe('Liga #999');
  });
});

describe('logoLiga', () => {
  test('URL-ul CDN pentru ligile obisnuite', () => {
    expect(logoLiga(39)).toBe('https://media.api-sports.io/football/leagues/39.png');
  });

  test('null pentru Campionatul Mondial (sursa are doar placeholder)', () => {
    expect(logoLiga(ID_CUPA_MONDIALA)).toBeNull();
  });
});

describe('idLigaDinLogo', () => {
  test('extrage id-ul din URL-ul de logo', () => {
    expect(idLigaDinLogo('https://media.api-sports.io/football/leagues/140.png')).toBe(140);
  });

  test('null cand URL-ul nu e un logo de liga', () => {
    expect(idLigaDinLogo('https://media.api-sports.io/football/teams/33.png')).toBeNull();
    expect(idLigaDinLogo('')).toBeNull();
  });
});

describe('idLigaEfectiv', () => {
  test('id-ul direct are prioritate fata de URL', () => {
    expect(idLigaEfectiv(39, 'https://media.api-sports.io/football/leagues/140.png')).toBe(39);
  });

  test('fara id: extras din URL; fara nimic: null', () => {
    expect(idLigaEfectiv(null, 'https://media.api-sports.io/football/leagues/140.png')).toBe(140);
    expect(idLigaEfectiv(null, null)).toBeNull();
    expect(idLigaEfectiv(undefined)).toBeNull();
  });
});

describe('esteLogoPlaceholder', () => {
  test('Campionatul Mondial e placeholder, direct sau prin URL', () => {
    expect(esteLogoPlaceholder(ID_CUPA_MONDIALA)).toBe(true);
    expect(esteLogoPlaceholder(null, 'https://media.api-sports.io/football/leagues/1.png')).toBe(true);
  });

  test('ligile obisnuite si lipsa oricarui id nu sunt placeholder', () => {
    expect(esteLogoPlaceholder(39)).toBe(false);
    expect(esteLogoPlaceholder(null, null)).toBe(false);
  });
});

describe('numeEchipa', () => {
  test('numele echipei sau fallback pe id', () => {
    expect(numeEchipa({ id: 33, nume: 'Manchester United', logo: null })).toBe('Manchester United');
    expect(numeEchipa({ id: 33, nume: null, logo: null })).toBe('Echipa #33');
  });
});
