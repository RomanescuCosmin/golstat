import { describe, expect, test } from 'vitest';
import { numeEchipa } from './echipa';
import {
  ID_CUPA_MONDIALA,
  esteLogoPlaceholder,
  idLigaDinLogo,
  idLigaEfectiv,
  logoLiga,
  numeLiga,
  sorteazaCompetitii,
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

describe('sorteazaCompetitii', () => {
  const ids = (ligi: { leagueId: number }[]) => ligi.map((l) => l.leagueId);

  test('top 5 campionate, apoi cupele, apoi restul, amicalele ultimele', () => {
    const intrare = [
      { leagueId: 667, nume: 'Amicale cluburi' },
      { leagueId: 283, nume: 'Liga I' },
      { leagueId: 135, nume: 'Serie A' },
      { leagueId: 2, nume: 'UEFA Champions League' },
      { leagueId: 39, nume: 'Premier League' },
    ];
    expect(ids(sorteazaCompetitii(intrare))).toEqual([39, 135, 2, 283, 667]);
  });

  test('stabila: ligile cu acelasi rang pastreaza ordinea de intrare (ora de start)', () => {
    const intrare = [
      { leagueId: 283, nume: 'Liga I' },
      { leagueId: 203, nume: 'Süper Lig' },
      { leagueId: 88, nume: 'Eredivisie' },
    ];
    expect(ids(sorteazaCompetitii(intrare))).toEqual([283, 203, 88]);
  });

  test('amicala cu id necunoscut e prinsa dupa nume', () => {
    const intrare = [
      { leagueId: 9999, nume: 'Club Friendlies' },
      { leagueId: 283, nume: 'Liga I' },
    ];
    expect(ids(sorteazaCompetitii(intrare))).toEqual([283, 9999]);
  });

  test('nu muteaza lista primita', () => {
    const intrare = [
      { leagueId: 667, nume: 'Amicale cluburi' },
      { leagueId: 39, nume: 'Premier League' },
    ];
    sorteazaCompetitii(intrare);
    expect(ids(intrare)).toEqual([667, 39]);
  });
});

describe('numeEchipa', () => {
  test('numele echipei sau fallback pe id', () => {
    expect(numeEchipa({ id: 33, nume: 'Manchester United', logo: null })).toBe('Manchester United');
    expect(numeEchipa({ id: 33, nume: null, logo: null })).toBe('Echipa #33');
  });
});
