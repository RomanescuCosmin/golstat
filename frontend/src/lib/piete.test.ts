import { describe, expect, it } from 'vitest';
import { cotaPiata, meciPiete, ziPiete } from '../test/factories';
import { codValid, etichetaZi, filtreaza, linieValida, numaraRanduri, piataDupaGrup } from './piete';

describe('filtreaza', () => {
  const zile = [
    ziPiete({
      data: '2026-07-21',
      meciuri: [
        meciPiete({ fixtureId: 1, piete: [cotaPiata({ probabilitate: 0.4 })] }),
        meciPiete({ fixtureId: 2, piete: [cotaPiata({ probabilitate: 0.8 })] }),
        meciPiete({ fixtureId: 3, piete: [cotaPiata({ probabilitate: 0.6 })] }),
      ],
    }),
    ziPiete({
      data: '2026-07-22',
      meciuri: [meciPiete({ fixtureId: 4, piete: [cotaPiata({ probabilitate: 0.2 })] })],
    }),
  ];

  it('sorteaza descrescator IN INTERIORUL fiecarei zile, nu global', () => {
    const rezultat = filtreaza(zile, 'GOLURI_PESTE', 2.5, 0);
    expect(rezultat.map((z) => z.data)).toEqual(['2026-07-21', '2026-07-22']);
    expect(rezultat[0].randuri.map((r) => r.meci.fixtureId)).toEqual([2, 3, 1]);
    expect(rezultat[1].randuri.map((r) => r.meci.fixtureId)).toEqual([4]);
  });

  it('elimina randurile sub prag si zilele ramase goale', () => {
    const rezultat = filtreaza(zile, 'GOLURI_PESTE', 2.5, 50);
    expect(rezultat).toHaveLength(1);
    expect(rezultat[0].data).toBe('2026-07-21');
    expect(rezultat[0].randuri.map((r) => r.meci.fixtureId)).toEqual([2, 3]);
  });

  it('pragul e INCLUSIV — 60% ramane la prag 60', () => {
    const rezultat = filtreaza(zile, 'GOLURI_PESTE', 2.5, 60);
    expect(rezultat[0].randuri.map((r) => r.meci.fixtureId)).toEqual([2, 3]);
  });

  it('ascunde meciurile fara esantion pe piata aleasa', () => {
    const fara = [
      ziPiete({
        data: '2026-07-21',
        meciuri: [
          meciPiete({ fixtureId: 1, piete: [cotaPiata({ piata: 'GOLURI_PESTE' })] }),
          meciPiete({ fixtureId: 2, piete: [] }),
        ],
      }),
    ];
    const rezultat = filtreaza(fara, 'CORNERE_PESTE', 8.5, 0);
    expect(rezultat).toHaveLength(0);
  });

  it('distinge liniile aceleiasi piete', () => {
    const multi = [
      ziPiete({
        meciuri: [
          meciPiete({
            fixtureId: 1,
            piete: [
              cotaPiata({ linie: 1.5, probabilitate: 0.9 }),
              cotaPiata({ linie: 2.5, probabilitate: 0.5 }),
            ],
          }),
        ],
      }),
    ];
    expect(filtreaza(multi, 'GOLURI_PESTE', 1.5, 0)[0].randuri[0].cota.probabilitate).toBe(0.9);
    expect(filtreaza(multi, 'GOLURI_PESTE', 2.5, 0)[0].randuri[0].cota.probabilitate).toBe(0.5);
  });

  it('pietele binare se cauta cu linie null', () => {
    const binar = [
      ziPiete({
        meciuri: [meciPiete({ piete: [cotaPiata({ piata: 'GG', linie: null, probabilitate: 0.55 })] })],
      }),
    ];
    expect(filtreaza(binar, 'GG', null, 0)[0].randuri[0].cota.probabilitate).toBe(0.55);
    expect(filtreaza(binar, 'GG', 2.5, 0)).toHaveLength(0);
  });

  it('sare peste probabilitatile ne-finite', () => {
    const stricat = [
      ziPiete({ meciuri: [meciPiete({ piete: [cotaPiata({ probabilitate: NaN })] })] }),
    ];
    expect(filtreaza(stricat, 'GOLURI_PESTE', 2.5, 0)).toHaveLength(0);
  });

  it('lista goala nu crapa', () => {
    expect(filtreaza([], 'GOLURI_PESTE', 2.5, 30)).toEqual([]);
    expect(numaraRanduri([])).toBe(0);
  });
});

describe('selectia de piata', () => {
  it('linieValida pastreaza linia cand exista in noua piata', () => {
    expect(linieValida('cornere', 8.5)).toBe(8.5);
  });

  it('linieValida cade pe prima linie cand cea curenta nu exista', () => {
    expect(linieValida('cornere', 2.5)).toBe(7.5);
  });

  it('linieValida da null la pietele binare', () => {
    expect(linieValida('gg', 2.5)).toBeNull();
    expect(linieValida('egaluri', null)).toBeNull();
  });

  it('codValid cade pe prima optiune a grupului', () => {
    expect(codValid('cornere', 'GOLURI_PESTE')).toBe('CORNERE_PESTE');
    expect(codValid('goluri', 'GOLURI_SUB')).toBe('GOLURI_SUB');
  });

  it('grup necunoscut cade pe prima piata', () => {
    expect(piataDupaGrup('inexistent').grup).toBe('goluri');
  });
});

describe('etichetaZi', () => {
  const azi = new Date(2026, 6, 20);

  it('marcheaza azi si maine', () => {
    expect(etichetaZi('2026-07-20', azi)).toBe('Azi');
    expect(etichetaZi('2026-07-21', azi)).toBe('Mâine');
    expect(etichetaZi('2026-07-22', azi)).toBeNull();
  });
});
