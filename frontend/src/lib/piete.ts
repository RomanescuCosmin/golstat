import type { CodPiata, CotaPiata, MeciPiete, ZiPiete } from '../api/types';

/**
 * Logica pură a paginii de piețe: ce piețe există, ce linii are fiecare, și cum se filtrează
 * lista după prag. Ținută separat de componente ca să fie testabilă direct — aici stă și singura
 * conversie între fracția 0..1 a serverului și pragul 0..100 al slider-ului.
 */

export interface DefinitiePiata {
  /** Grupul din selectorul de piață. */
  grup: string;
  eticheta: string;
  /** Codurile pe care le acoperă grupul (ex. Goluri = peste + sub). */
  optiuni: { cod: CodPiata; eticheta: string }[];
  /** Liniile disponibile; listă goală = piață binară, fără selector de linie. */
  linii: number[];
}

/** Liniile trebuie să reflecte constantele din `StatisticiAvansateBuilder` (backend). */
export const PIETE: DefinitiePiata[] = [
  {
    grup: 'goluri',
    eticheta: 'Goluri',
    optiuni: [
      { cod: 'GOLURI_PESTE', eticheta: 'Peste' },
      { cod: 'GOLURI_SUB', eticheta: 'Sub' },
    ],
    linii: [1.5, 2.5, 3.5],
  },
  {
    grup: 'gg',
    eticheta: 'GG',
    optiuni: [
      { cod: 'GG', eticheta: 'Ambele înscriu' },
      { cod: 'NG', eticheta: 'Nu ambele' },
    ],
    linii: [],
  },
  {
    grup: 'cornere',
    eticheta: 'Cornere',
    optiuni: [{ cod: 'CORNERE_PESTE', eticheta: 'Peste' }],
    linii: [7.5, 8.5, 9.5, 10.5],
  },
  {
    grup: 'cartonase',
    eticheta: 'Cartonașe',
    optiuni: [{ cod: 'CARTONASE_PESTE', eticheta: 'Peste' }],
    linii: [3.5, 4.5, 5.5],
  },
  {
    grup: 'faulturi',
    eticheta: 'Faulturi',
    optiuni: [{ cod: 'FAULTURI_PESTE', eticheta: 'Peste' }],
    linii: [21.5, 24.5, 27.5],
  },
  {
    grup: 'egaluri',
    eticheta: 'Egaluri',
    optiuni: [
      { cod: 'EGAL_FINAL', eticheta: 'Egal la final' },
      { cod: 'EGAL_PAUZA', eticheta: 'Egal la pauză' },
    ],
    linii: [],
  },
];

export function piataDupaGrup(grup: string): DefinitiePiata {
  return PIETE.find((p) => p.grup === grup) ?? PIETE[0];
}

/** Linia validă după schimbarea pieței: păstrează selecția dacă există, altfel prima linie. */
export function linieValida(grup: string, linie: number | null): number | null {
  const { linii } = piataDupaGrup(grup);
  if (linii.length === 0) return null;
  return linie != null && linii.includes(linie) ? linie : linii[0];
}

/** Codul valid după schimbarea pieței: păstrează selecția dacă aparține grupului, altfel primul. */
export function codValid(grup: string, cod: CodPiata | null): CodPiata {
  const { optiuni } = piataDupaGrup(grup);
  return cod != null && optiuni.some((o) => o.cod === cod) ? cod : optiuni[0].cod;
}

/** Piața cerută dintr-un meci; `undefined` când meciul n-are eșantion pe ea. */
export function cotaMeciului(meci: MeciPiete, cod: CodPiata, linie: number | null): CotaPiata | undefined {
  return meci.piete.find((p) => p.piata === cod && (linie == null ? p.linie == null : p.linie === linie));
}

export interface RandPiete {
  meci: MeciPiete;
  cota: CotaPiata;
}

export interface ZiFiltrata {
  data: string;
  randuri: RandPiete[];
}

/**
 * Filtrează pe piață + prag și sortează descrescător ÎN INTERIORUL fiecărei zile. Meciurile fără
 * eșantion pe piața aleasă dispar; zilele rămase goale nu se mai returnează.
 *
 * `prag` e 0..100 (slider), `probabilitate` e 0..1 (server) — conversia se face DOAR aici.
 * Comparația e inclusivă: pragul 30 păstrează exact 30%.
 */
export function filtreaza(
  zile: ZiPiete[],
  cod: CodPiata,
  linie: number | null,
  prag: number,
): ZiFiltrata[] {
  const rezultat: ZiFiltrata[] = [];
  for (const zi of zile) {
    const randuri: RandPiete[] = [];
    for (const meci of zi.meciuri) {
      const cota = cotaMeciului(meci, cod, linie);
      if (!cota || !Number.isFinite(cota.probabilitate)) continue;
      if (cota.probabilitate * 100 < prag) continue;
      randuri.push({ meci, cota });
    }
    if (randuri.length === 0) continue;
    randuri.sort(
      (a, b) => b.cota.probabilitate - a.cota.probabilitate || a.meci.kickoff.localeCompare(b.meci.kickoff),
    );
    rezultat.push({ data: zi.data, randuri });
  }
  return rezultat;
}

export function numaraRanduri(zile: ZiFiltrata[]): number {
  return zile.reduce((total, zi) => total + zi.randuri.length, 0);
}

/** Eticheta unei zile: „Azi" / „Mâine" pentru primele două, altfel data scurtă. */
export function etichetaZi(dataISO: string, azi = new Date()): string | null {
  const aziISO = iso(azi);
  const maine = new Date(azi);
  maine.setDate(maine.getDate() + 1);
  if (dataISO === aziISO) return 'Azi';
  if (dataISO === iso(maine)) return 'Mâine';
  return null;
}

function iso(d: Date): string {
  const luna = String(d.getMonth() + 1).padStart(2, '0');
  const zi = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${luna}-${zi}`;
}
