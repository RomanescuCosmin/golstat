import { describe, expect, it } from 'vitest';
import { cotaPiata, meciPiete, ziPiete } from '../test/factories';
import {
  codValid,
  etichetaZi,
  filtreaza,
  ligiDisponibile,
  ligiValide,
  linieValida,
  numaraRanduri,
  piataDupaGrup,
} from './piete';

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

describe('filtrul de campionate', () => {
  const zile = [
    ziPiete({
      data: '2026-07-21',
      meciuri: [
        meciPiete({
          fixtureId: 1,
          liga: { id: 39, nume: 'Premier League', logo: null },
          piete: [cotaPiata({ probabilitate: 0.8 })],
        }),
        meciPiete({
          fixtureId: 2,
          liga: { id: 140, nume: 'La Liga', logo: null },
          piete: [cotaPiata({ probabilitate: 0.7 })],
        }),
        meciPiete({
          fixtureId: 3,
          liga: { id: 667, nume: 'Friendlies Clubs', logo: null },
          piete: [cotaPiata({ probabilitate: 0.2 })],
        }),
      ],
    }),
  ];

  it('lista goala inseamna TOATE campionatele, nu niciunul', () => {
    expect(numaraRanduri(filtreaza(zile, 'GOLURI_PESTE', 2.5, 0, []))).toBe(3);
  });

  it('pastreaza doar campionatele alese', () => {
    const rezultat = filtreaza(zile, 'GOLURI_PESTE', 2.5, 0, [39]);
    expect(rezultat[0].randuri.map((r) => r.meci.fixtureId)).toEqual([1]);
  });

  it('accepta selectii multiple', () => {
    const rezultat = filtreaza(zile, 'GOLURI_PESTE', 2.5, 0, [39, 140]);
    expect(rezultat[0].randuri.map((r) => r.meci.fixtureId)).toEqual([1, 2]);
  });

  it('se combina cu pragul', () => {
    expect(numaraRanduri(filtreaza(zile, 'GOLURI_PESTE', 2.5, 75, [39, 140]))).toBe(1);
    expect(numaraRanduri(filtreaza(zile, 'GOLURI_PESTE', 2.5, 0, [667]))).toBe(1);
    expect(numaraRanduri(filtreaza(zile, 'GOLURI_PESTE', 2.5, 50, [667]))).toBe(0);
  });

  it('campionat inexistent da lista goala, nu toate', () => {
    expect(filtreaza(zile, 'GOLURI_PESTE', 2.5, 0, [999])).toEqual([]);
  });

  it('ligiDisponibile numara dupa piata si prag, sortat descrescator', () => {
    const toate = ligiDisponibile(zile, 'GOLURI_PESTE', 2.5, 0);
    expect(toate.map((l) => [l.id, l.numar])).toEqual([
      [667, 1],
      [140, 1],
      [39, 1],
    ]);

    const pesteJumate = ligiDisponibile(zile, 'GOLURI_PESTE', 2.5, 50);
    // campionatele fara meciuri raman in lista (cu 0), ca sa nu sara pastilele
    expect(pesteJumate).toHaveLength(3);
    expect(pesteJumate.find((l) => l.id === 667)?.numar).toBe(0);
    expect(pesteJumate[0].numar).toBe(1);
  });

  it('ligiDisponibile NU tine cont de selectia de campionate', () => {
    // altfel alegerea unui campionat ar arata 0 la toate celelalte
    const lista = ligiDisponibile(zile, 'GOLURI_PESTE', 2.5, 0);
    expect(lista.every((l) => l.numar === 1)).toBe(true);
  });

  it('ligiValide curata campionatele disparute din date', () => {
    const disponibile = ligiDisponibile(zile, 'GOLURI_PESTE', 2.5, 0);
    expect(ligiValide([39, 999], disponibile)).toEqual([39]);
    expect(ligiValide([], disponibile)).toEqual([]);
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
