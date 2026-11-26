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
  /** Pictograma din selectorul de tip; ține locul unui set de iconițe dedicat. */
  icona: string;
  /** Substantivul din eticheta lungă („peste 2.5 goluri"); lipsă la piețele binare. */
  unitate?: string;
  /** Codurile pe care le acoperă grupul (ex. Goluri = peste + sub). */
  optiuni: { cod: CodPiata; eticheta: string }[];
  /** Liniile disponibile; listă goală = piață binară, fără linie. */
  linii: number[];
  /** „Peste X și sub Y": utilizatorul alege ambele linii din `linii` (X < Y). */
  interval?: boolean;
  /**
   * Intervalul se alege în numere întregi („între 1 și 3”): limitele rămân liniile x.5 de sub capot
   * (1–3 = peste 0.5 și sub 3.5), dar se afișează ca „De la 1” / „Până la 3”.
   */
  intervalIntreg?: boolean;
  /** Intervalul ales când nu există o alegere validă (ex. 1–3 goluri = [0.5, 3.5]). */
  intervalImplicit?: [number, number];
}

/** Limita de jos a unui interval întreg („de la a”) din linia de sub capot: a = linie + 0.5. */
export function delaDinLinie(linie: number): number {
  return linie + 0.5;
}

/** Limita de sus a unui interval întreg („până la b”): b = linie − 0.5. */
export function panaLaDinLinie(linie: number): number {
  return linie - 0.5;
}

/**
 * Piețele-interval nu vin de la server: se derivă din liniile „peste" ale aceleiași reprize,
 * P(peste X și sub Y) = P(peste X) − P(peste Y) — șansa ca AMBELE să iasă, nu media lor.
 */
const SURSA_INTERVAL: Partial<Record<CodPiata, CodPiata>> = {
  CORNERE_REPRIZA1_INTERVAL: 'CORNERE_REPRIZA1_PESTE',
  CORNERE_REPRIZA2_INTERVAL: 'CORNERE_REPRIZA2_PESTE',
  GOLURI_INTERVAL: 'GOLURI_PESTE',
};

/** Liniile trebuie să reflecte constantele din `StatisticiAvansateBuilder` (backend). */
export const PIETE: DefinitiePiata[] = [
  {
    grup: 'goluri',
    eticheta: 'Goluri',
    icona: '⚽',
    unitate: 'goluri',
    optiuni: [
      { cod: 'GOLURI_PESTE', eticheta: 'Peste' },
      { cod: 'GOLURI_SUB', eticheta: 'Sub' },
    ],
    linii: [1.5, 2.5, 3.5],
  },
  {
    grup: 'intervalGoluri',
    eticheta: 'Interval goluri',
    icona: '🎯',
    unitate: 'goluri',
    optiuni: [{ cod: 'GOLURI_INTERVAL', eticheta: 'Total meci' }],
    // −0.5 = „de la 0 goluri” (P(peste −0.5) = 1); 6.5 = „până la 6 goluri”
    linii: [-0.5, 0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5],
    interval: true,
    intervalIntreg: true,
    intervalImplicit: [0.5, 3.5],
  },
  {
    grup: 'gg',
    eticheta: 'GG',
    icona: '🤝',
    optiuni: [
      { cod: 'GG', eticheta: 'Ambele înscriu' },
      { cod: 'NG', eticheta: 'Nu ambele' },
    ],
    linii: [],
  },
  {
    grup: 'cornere',
    eticheta: 'Cornere',
    icona: '🚩',
    unitate: 'cornere',
    optiuni: [{ cod: 'CORNERE_PESTE', eticheta: 'Peste' }],
    linii: [7.5, 8.5, 9.5, 10.5],
  },
  {
    grup: 'cartonase',
    eticheta: 'Cartonașe',
    icona: '🟨',
    unitate: 'cartonașe',
    optiuni: [{ cod: 'CARTONASE_PESTE', eticheta: 'Peste' }],
    linii: [3.5, 4.5, 5.5],
  },
  {
    grup: 'goluriEchipa',
    eticheta: 'Goluri pe echipă',
    icona: '⚽',
    unitate: 'goluri',
    optiuni: [
      { cod: 'GOLURI_GAZDE_PESTE', eticheta: 'Gazdele peste' },
      { cod: 'GOLURI_OASPETI_PESTE', eticheta: 'Oaspeții peste' },
    ],
    linii: [0.5, 1.5, 2.5],
  },
  {
    grup: 'goluriReprize',
    eticheta: 'Gol în prima / a doua',
    icona: '⏱️',
    unitate: 'goluri',
    optiuni: [
      { cod: 'GOLURI_REPRIZA1_PESTE', eticheta: 'Prima repriză peste' },
      { cod: 'GOLURI_REPRIZA2_PESTE', eticheta: 'A doua repriză peste' },
    ],
    linii: [0.5, 1.5],
  },
  {
    grup: 'cornereEchipa',
    eticheta: 'Cornere pe echipă',
    icona: '🚩',
    unitate: 'cornere',
    optiuni: [
      { cod: 'CORNERE_GAZDE_PESTE', eticheta: 'Gazdele peste' },
      { cod: 'CORNERE_OASPETI_PESTE', eticheta: 'Oaspeții peste' },
    ],
    linii: [2.5, 3.5, 4.5, 5.5],
  },
  {
    grup: 'cornereReprize',
    eticheta: 'Cornere pe reprize',
    icona: '🚩',
    unitate: 'cornere',
    optiuni: [
      { cod: 'CORNERE_REPRIZA1_PESTE', eticheta: 'Repriza 1 peste' },
      { cod: 'CORNERE_REPRIZA2_PESTE', eticheta: 'Repriza 2 peste' },
      { cod: 'CORNERE_REPRIZA1_SUB', eticheta: 'Repriza 1 sub' },
      { cod: 'CORNERE_REPRIZA2_SUB', eticheta: 'Repriza 2 sub' },
    ],
    linii: [2.5, 3.5, 4.5, 5.5, 6.5],
  },
  {
    grup: 'intervalCornereReprize',
    eticheta: 'Interval cornere pe repriză',
    icona: '🎯',
    unitate: 'cornere',
    optiuni: [
      { cod: 'CORNERE_REPRIZA1_INTERVAL', eticheta: 'Repriza 1' },
      { cod: 'CORNERE_REPRIZA2_INTERVAL', eticheta: 'Repriza 2' },
    ],
    linii: [2.5, 3.5, 4.5, 5.5, 6.5],
    interval: true,
  },
  {
    grup: 'cartonaseEchipa',
    eticheta: 'Cartonașe pe echipă',
    icona: '🟨',
    unitate: 'cartonașe',
    optiuni: [
      { cod: 'CARTONASE_GAZDE_PESTE', eticheta: 'Gazdele peste' },
      { cod: 'CARTONASE_OASPETI_PESTE', eticheta: 'Oaspeții peste' },
    ],
    linii: [0.5, 1.5, 2.5],
  },
  {
    grup: 'faulturi',
    eticheta: 'Faulturi',
    icona: '⚠️',
    unitate: 'faulturi',
    optiuni: [{ cod: 'FAULTURI_PESTE', eticheta: 'Peste' }],
    linii: [21.5, 24.5, 27.5],
  },
  {
    grup: 'egaluri',
    eticheta: 'Egaluri',
    icona: '➕',
    optiuni: [
      { cod: 'EGAL_FINAL', eticheta: 'Egal la final' },
      { cod: 'EGAL_PAUZA', eticheta: 'Egal la pauză' },
    ],
    linii: [],
  },
];

export interface OptiunePiata {
  /** Cheia din `<select>`: direcția și linia la un loc. */
  valoare: string;
  eticheta: string;
  cod: CodPiata;
  linie: number | null;
}

/** Cheia unei selecții (cod + linie), ca dropdown-ul să lucreze cu o singură valoare. */
export function valoareOptiune(cod: CodPiata, linie: number | null): string {
  return `${cod}|${linie ?? ''}`;
}

/**
 * Toate selecțiile unui grup, aplatizate pentru dropdown: direcțiile × liniile
 * („Peste 1.5", „Peste 2.5", …). Piețele binare dau o opțiune per direcție.
 */
export function optiuniPiata(grup: string): OptiunePiata[] {
  const { optiuni, linii, interval } = piataDupaGrup(grup);
  return optiuni.flatMap<OptiunePiata>((o) =>
    linii.length === 0 || interval
      ? [{ valoare: valoareOptiune(o.cod, null), eticheta: o.eticheta, cod: o.cod, linie: null }]
      : linii.map((l) => ({
          valoare: valoareOptiune(o.cod, l),
          eticheta: `${o.eticheta} ${l}`,
          cod: o.cod,
          linie: l,
        })),
  );
}

export function optiuneDupaValoare(grup: string, valoare: string): OptiunePiata {
  const optiuni = optiuniPiata(grup);
  return optiuni.find((o) => o.valoare === valoare) ?? optiuni[0];
}

/** Eticheta lungă, pentru pastila din rândul de meci: „Peste 2.5 goluri". */
export function etichetaPiata(
  grup: string,
  cod: CodPiata,
  linie: number | null,
  linieSus: number | null = null,
): string {
  const definitie = piataDupaGrup(grup);
  const optiune = definitie.optiuni.find((o) => o.cod === cod) ?? definitie.optiuni[0];
  if (definitie.intervalIntreg && linie != null && linieSus != null) {
    const a = delaDinLinie(linie);
    const b = panaLaDinLinie(linieSus);
    return a === b ? `Exact ${a} ${definitie.unitate}` : `Între ${a} și ${b} ${definitie.unitate}`;
  }
  if (definitie.interval && linie != null && linieSus != null) {
    return `${optiune.eticheta}: peste ${linie} și sub ${linieSus} ${definitie.unitate}`;
  }
  if (linie == null) return optiune.eticheta;
  return definitie.unitate
    ? `${optiune.eticheta} ${linie} ${definitie.unitate}`
    : `${optiune.eticheta} ${linie}`;
}

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

/**
 * Intervalul valid după o schimbare: păstrează alegerea dacă e corectă (X < Y, ambele în listă),
 * altfel prima și ultima linie (la cornere pe repriză: peste 2.5 și sub 6.5).
 */
export function intervalValid(grup: string, jos: number | null, sus: number | null): [number, number] | null {
  const { linii, interval, intervalImplicit } = piataDupaGrup(grup);
  if (!interval || linii.length < 2) return null;
  if (intervalImplicit) {
    // ambele limite vin împreună: „peste 2.5” rămas de la altă piață nu e o alegere de interval
    const valid = jos != null && sus != null && linii.includes(jos) && linii.includes(sus) && sus > jos;
    return valid ? [jos, sus] : intervalImplicit;
  }
  const josOk = jos != null && linii.includes(jos) && jos < linii[linii.length - 1] ? jos : linii[0];
  const susOk = sus != null && linii.includes(sus) && sus > josOk ? sus : linii[linii.length - 1];
  return [josOk, susOk];
}

/** Piața cerută dintr-un meci; `undefined` când meciul n-are eșantion pe ea. */
export function cotaMeciului(
  meci: MeciPiete,
  cod: CodPiata,
  linie: number | null,
  linieSus: number | null = null,
): CotaPiata | undefined {
  const sursa = SURSA_INTERVAL[cod];
  if (sursa) return cotaInterval(meci, cod, sursa, linie, linieSus);
  return meci.piete.find((p) => p.piata === cod && (linie == null ? p.linie == null : p.linie === linie));
}

function cotaInterval(
  meci: MeciPiete,
  cod: CodPiata,
  sursa: CodPiata,
  jos: number | null,
  sus: number | null,
): CotaPiata | undefined {
  if (jos == null || sus == null || sus <= jos) return undefined;
  // „peste −0.5” = sigur (de la 0 goluri); eșantionul îl luăm de la orice linie a sursei
  const peste =
    jos < 0
      ? withProbabilitate(meci.piete.find((p) => p.piata === sursa), 1)
      : meci.piete.find((p) => p.piata === sursa && p.linie === jos);
  // serverul nu trimite liniile cu P = 0: lipsa liniei de sus înseamnă P(peste Y) = 0
  const pesteSus = meci.piete.find((p) => p.piata === sursa && p.linie === sus);
  if (!peste) return undefined;
  const probabilitate = peste.probabilitate - (pesteSus?.probabilitate ?? 0);
  if (!Number.isFinite(probabilitate) || probabilitate <= 0) return undefined;
  return {
    piata: cod,
    linie: jos,
    probabilitate,
    cota: Math.round(100 / probabilitate) / 100,
    esantion: peste.esantion,
  };
}

function withProbabilitate(cota: CotaPiata | undefined, probabilitate: number): CotaPiata | undefined {
  return cota && { ...cota, probabilitate };
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
 * Filtrează pe piață + prag (+ campionatele alese) și sortează descrescător ÎN INTERIORUL fiecărei
 * zile. Meciurile fără eșantion pe piața aleasă dispar; zilele rămase goale nu se mai returnează.
 *
 * `prag` e 0..100 (slider), `probabilitate` e 0..1 (server) — conversia se face DOAR aici.
 * Comparația e inclusivă: pragul 30 păstrează exact 30%.
 * `ligi` gol = toate campionatele (fără filtru), nu „niciunul".
 */
export function filtreaza(
  zile: ZiPiete[],
  cod: CodPiata,
  linie: number | null,
  prag: number,
  ligi: number[] = [],
  linieSus: number | null = null,
): ZiFiltrata[] {
  const alese = ligi.length > 0 ? new Set(ligi) : null;
  const rezultat: ZiFiltrata[] = [];
  for (const zi of zile) {
    const randuri: RandPiete[] = [];
    for (const meci of zi.meciuri) {
      if (alese && !alese.has(meci.liga.id)) continue;
      const cota = cotaMeciului(meci, cod, linie, linieSus);
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

export interface OptiuneLiga {
  id: number;
  nume: string | null;
  logo: string | null;
  /** Câte meciuri trec piața + pragul curent (fără filtrul de campionat). */
  numar: number;
}

/**
 * Campionatele din fereastră, cu câte meciuri are fiecare la piața și pragul curente.
 *
 * Lista se construiește din TOATE meciurile aduse, nu doar din cele care trec pragul — altfel
 * pastilele ar apărea și dispărea în timp ce tragi slider-ul. Cele rămase la 0 cad la coadă.
 * Selecția de campionate nu intră în numărătoare, altfel alegerea unuia le-ar goli pe restul.
 */
export function ligiDisponibile(
  zile: ZiPiete[],
  cod: CodPiata,
  linie: number | null,
  prag: number,
  linieSus: number | null = null,
): OptiuneLiga[] {
  const sursa = SURSA_INTERVAL[cod] ?? cod;
  const perLiga = new Map<number, OptiuneLiga>();
  for (const zi of zile) {
    for (const meci of zi.meciuri) {
      // campionatul fara statistica pietei (ex. cornere pe reprize la National League) nu apare in filtru
      if (!meci.piete.some((p) => p.piata === sursa)) continue;
      const existent = perLiga.get(meci.liga.id);
      const optiune = existent ?? {
        id: meci.liga.id,
        nume: meci.liga.nume,
        logo: meci.liga.logo,
        numar: 0,
      };
      const cota = cotaMeciului(meci, cod, linie, linieSus);
      if (cota && Number.isFinite(cota.probabilitate) && cota.probabilitate * 100 >= prag) {
        optiune.numar += 1;
      }
      if (!existent) perLiga.set(meci.liga.id, optiune);
    }
  }
  return [...perLiga.values()].sort(
    (a, b) => b.numar - a.numar || (a.nume ?? '').localeCompare(b.nume ?? ''),
  );
}

/** Selecția curată după ce datele s-au schimbat: păstrează doar campionatele care încă există. */
export function ligiValide(ligi: number[], disponibile: OptiuneLiga[]): number[] {
  const existente = new Set(disponibile.map((l) => l.id));
  return ligi.filter((id) => existente.has(id));
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

/** Ferestrele orare din filtrul paginii de piețe („în următoarele N ore"). */
export const FERESTRE_ORE = [3, 5, 8] as const;

/**
 * Păstrează doar meciurile care încep în următoarele `ore` ore (de la `acum`); cele deja începute
 * ies. `ore = null` = fără filtru. Zilele rămase goale dispar.
 */
export function inFereastra(zile: ZiPiete[], ore: number | null, acum: number = Date.now()): ZiPiete[] {
  if (ore == null) return zile;
  const limita = acum + ore * 60 * 60 * 1000;
  return zile
    .map((zi) => ({
      ...zi,
      meciuri: zi.meciuri.filter((m) => {
        const t = new Date(m.kickoff).getTime();
        return t >= acum && t <= limita;
      }),
    }))
    .filter((zi) => zi.meciuri.length > 0);
}
