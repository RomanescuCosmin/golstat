import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test } from 'vitest';
import type {
  EchipaDto,
  FrecventaDto,
  GgDto,
  LinieStatDto,
  MediiEchipaDto,
  PiataStatDto,
  RezultatStatisticiDto,
  StatisticiAvansateDto,
} from '../../api/types';
import { SectiuneStatistici } from './SectiuneStatistici';
import {
  formaEchipa,
  formaMeci,
  fereastraForma,
  piataEchipa,
  rezultatEchipe,
  statisticiAvansate,
} from '../../test/factories';

/* ──────────────── fixtures ──────────────── */

const GAZDE: EchipaDto = { id: 1, nume: 'FCSB', logo: null };
const OASPETI: EchipaDto = { id: 2, nume: 'CFR Cluj', logo: null };

const f = (reusite: number, total: number): FrecventaDto => ({ reusite, total });

const medii = (
  proprieLocatie: number | null,
  totalLocatie: number | null,
  proprieGeneral: number | null = null,
): MediiEchipaDto => ({ proprieLocatie, totalLocatie, proprieGeneral, totalGeneral: null });

function linie(l: number, probabilitate: number): LinieStatDto {
  return {
    linie: l,
    probabilitate,
    gazdeLocatie: f(5, 7),
    gazdeGeneral: f(4, 7),
    oaspetiLocatie: f(3, 7),
    oaspetiGeneral: f(2, 7),
  };
}

/** O linie complet fara istoric: toate frecventele 0/0 — probabilitatea vine doar din media ligii. */
function linieFaraIstoric(l: number, probabilitate: number): LinieStatDto {
  return {
    linie: l,
    probabilitate,
    gazdeLocatie: f(0, 0),
    gazdeGeneral: f(0, 0),
    oaspetiLocatie: f(0, 0),
    oaspetiGeneral: f(0, 0),
  };
}

const piata = (linii: LinieStatDto[]): PiataStatDto => ({
  linii,
  gazde: medii(1.9, 3.1, 1.7),
  oaspeti: medii(1.2, 2.6),
});

const GG: GgDto = {
  probabilitate: 0.58,
  gazdeMarcat: f(6, 7),
  gazdePrimit: f(4, 7),
  oaspetiMarcat: f(5, 7),
  oaspetiPrimit: f(3, 7),
};

function statistici(over: Partial<StatisticiAvansateDto> = {}): StatisticiAvansateDto {
  return {
    goluri: piata([linie(2.5, 0.62)]),
    gg: GG,
    cornere: piata([linie(9.5, 0.55)]),
    faulturi: piata([linie(24.5, 0.48)]),
    cartonase: piata([linie(4.5, 0.51)]),
    suturi: piata([linie(22.5, 0.6)]),
    suturiPePoarta: piata([linie(8.5, 0.44)]),
    egaluri: {
      egalPauza: 0.41,
      egalFinal: 0.27,
      pauzaGazde: f(3, 7),
      pauzaOaspeti: f(2, 7),
      finalGazde: f(1, 7),
      finalOaspeti: f(2, 7),
    },
    reprize: {
      golRepriza1: 0.71,
      golRepriza2: 0.83,
      repriza1Gazde: f(5, 7),
      repriza1Oaspeti: f(4, 7),
      repriza2Gazde: f(6, 7),
      repriza2Oaspeti: f(5, 7),
      peste15Repriza1: 0.3,
      peste15Repriza2: 0.4,
    },
    rezultat: null,
    echipe: statisticiAvansate().echipe,
    cornereReprize: { ...statisticiAvansate().cornereReprize, echipeRepriza1: piataEchipa() },
    ...over,
  };
}

const REZULTAT: RezultatStatisticiDto = {
  totalGoluri: 3,
  ambeleMarcheaza: true,
  egalFinal: false,
  egalPauza: null,
  golRepriza1: true,
  golRepriza2: null,
  totalCornere: 9,
  totalFaulturi: null,
  totalCartonase: null,
  totalSuturi: null,
  totalSuturiPePoarta: null,
  echipe: rezultatEchipe(),
};


function randeaza(s: StatisticiAvansateDto, extra: Partial<Parameters<typeof SectiuneStatistici>[0]> = {}) {
  return render(<SectiuneStatistici statistici={s} gazde={GAZDE} oaspeti={OASPETI} {...extra} />);
}

async function tab(user: ReturnType<typeof userEvent.setup>, nume: string) {
  await user.click(screen.getByRole('tab', { name: nume }));
}

describe('SectiuneStatistici — Comparație (implicit)', () => {
  test('echipele fata in fata, procentele fiecareia si sansele meciului', () => {
    randeaza(statistici());
    expect(screen.getByRole('tab', { name: 'Comparație' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getAllByText('FCSB').length).toBeGreaterThan(0);
    expect(screen.getAllByText('CFR Cluj').length).toBeGreaterThan(0);
    // peste 2.5 goluri: 5/7 la gazde, 3/7 la oaspeti
    expect(screen.getAllByText('71%').length).toBeGreaterThan(0);
    expect(screen.getByText('Șansele meciului')).toBeInTheDocument();
    expect(screen.getByText(/— 62%/)).toBeInTheDocument();
  });

  test('meci terminat: verdict ✓ pe sansele meciului', () => {
    randeaza(statistici({ rezultat: REZULTAT }));
    // 2.5 goluri la 62% si 3 goluri reale → ✓
    expect(screen.getAllByText((_, el) => el?.textContent?.trim() === '✓ Da').length).toBeGreaterThan(0);
  });
});

describe('SectiuneStatistici — taburi pe piete', () => {
  test('Goluri: panoul fiecarei echipe cu ultimele rezultate si liniile ei', async () => {
    const user = userEvent.setup();
    randeaza(statistici(), {
      formaGazde: formaEchipa({ locatie: fereastraForma({ meciuri: [formaMeci({ golMarcate: 3, golPrimite: 1 })] }) }),
    });
    await tab(user, 'Goluri');
    expect(screen.getByText('Ultimele 7 rezultate acasă')).toBeInTheDocument();
    expect(screen.getByText('3-1')).toBeInTheDocument();
    expect(screen.getAllByText(/Peste 0.5 goluri/).length).toBeGreaterThan(0);
    expect(screen.getByText('Goluri în meci')).toBeInTheDocument();
  });

  test('fara istoric: procentul nu se afiseaza („—")', async () => {
    const user = userEvent.setup();
    randeaza(statistici({ cornere: piata([linieFaraIstoric(9.5, 0.6)]) }));
    await tab(user, 'Cornere');
    expect(screen.getByText('Cornere în meci')).toBeInTheDocument();
    expect(screen.getAllByText(/— —/).length).toBeGreaterThan(0);
  });

  test('Cornere: si pe reprize', async () => {
    const user = userEvent.setup();
    randeaza(statistici());
    await tab(user, 'Cornere');
    expect(screen.getByText('Cornere în repriza 1')).toBeInTheDocument();
    expect(screen.getByText('Cornere în repriza 2')).toBeInTheDocument();
  });

  test('Cartonașe: cardul arbitrului', async () => {
    const user = userEvent.setup();
    randeaza(statistici(), {
      arbitru: {
        nume: 'I. Kovacs', meciuri: 12, cartonasePeMeci: 5.4, faulturiPeMeci: 26.1, eliminari: 2,
        cartonaseMediaLigii: 4.5, factorCartonase: 1.15,
      },
    });
    await tab(user, 'Cartonașe');
    expect(screen.getByText('⚖️ Arbitru: I. Kovacs')).toBeInTheDocument();
    expect(screen.getByText(/sever: \+15% la cartonașe/)).toBeInTheDocument();
  });

  test('niciun tab nu afiseaza NaN / undefined', async () => {
    const user = userEvent.setup();
    const { container } = randeaza(statistici({ rezultat: REZULTAT }));
    for (const t of screen.getAllByRole('tab')) {
      await user.click(t);
      expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
    }
  });

  test('comutatorul de competitie anunta alegerea', async () => {
    const user = userEvent.setup();
    const alegeri: string[] = [];
    randeaza(statistici(), { onCompetitie: (v) => alegeri.push(v) });
    await user.click(screen.getByRole('button', { name: 'Doar competiția meciului' }));
    expect(alegeri).toEqual(['curenta']);
  });
});

describe('SectiuneStatistici — cornere pe reprize: peste 2.5 … 5.5 si sub 6.5', () => {
  const reprize = () =>
    statistici({
      cornereReprize: {
        ...statistici().cornereReprize!,
        repriza1: piata([
          linie(2.5, 0.875),
          linie(3.5, 0.734),
          linie(4.5, 0.558),
          linie(5.5, 0.383),
          { ...linie(6.5, 0.237), gazdeLocatie: f(1, 7), oaspetiLocatie: f(2, 6) },
        ]),
        repriza2: piata([linie(2.5, 0.9), linie(6.5, 0.3)]),
      },
    });

  const card = (titlu: string) => screen.getByText(titlu).closest('[class*="p-5"]') as HTMLElement;

  test('repriza 1: liniile peste 2.5–5.5 si „Sub 6.5" (fara „Peste 6.5")', async () => {
    const user = userEvent.setup();
    randeaza(reprize());
    await tab(user, 'Cornere');
    const r1 = within(card('Cornere în repriza 1'));
    for (const l of ['2.5', '3.5', '4.5', '5.5']) {
      expect(r1.getByText(new RegExp(`^Peste ${l} cornere`))).toBeInTheDocument();
    }
    expect(r1.getByText(/^Sub 6.5 cornere/)).toBeInTheDocument();
    expect(r1.queryByText(/^Peste 6.5 cornere/)).toBeNull();
    expect(within(card('Cornere în repriza 2')).getByText(/^Sub 6.5 cornere/)).toBeInTheDocument();
  });

  test('„Sub 6.5" = 1 − P(peste 6.5) si frecventele inversate', async () => {
    const user = userEvent.setup();
    randeaza(reprize());
    await tab(user, 'Cornere');
    const r1 = within(card('Cornere în repriza 1'));
    expect(r1.getByText(/^Sub 6.5 cornere/)).toHaveTextContent('— 76%');
    // peste 6.5 in 1/7 acasa si 2/6 in deplasare → sub 6.5 in 6/7 si 4/6
    expect(r1.getByText('6/7 · 4/6')).toBeInTheDocument();
  });

  test('statisticile nu mai afiseaza intervalul „Între 3 și 6" (e doar pe Piețe)', async () => {
    const user = userEvent.setup();
    randeaza(reprize());
    await tab(user, 'Cornere');
    expect(screen.queryByText(/Între/)).toBeNull();
  });

  test('meci terminat: ✓/✗ pe totalul real al reprizei (R1 = 4 cornere)', async () => {
    const user = userEvent.setup();
    // rezultatEchipe(): R1 = 2 + 2 = 4 cornere
    randeaza({ ...reprize(), rezultat: REZULTAT });
    await tab(user, 'Cornere');
    const r1 = within(card('Cornere în repriza 1'));
    expect(r1.getByText('Real: 4')).toBeInTheDocument();
    const rand = (eticheta: RegExp) => r1.getByText(eticheta).closest('.py-2') as HTMLElement;
    expect(rand(/^Peste 2.5 cornere/)).toHaveTextContent('✓ Da');
    expect(rand(/^Peste 4.5 cornere/)).toHaveTextContent('✗ Nu');
    expect(rand(/^Sub 6.5 cornere/)).toHaveTextContent('✓ Da');
  });
});

describe('SectiuneStatistici — Goluri: rezultate cu emblemele ambelor echipe', () => {
  test('fiecare rand are emblema echipei si a adversarului, scorul in ordinea gazde-oaspeti', async () => {
    const user = userEvent.setup();
    randeaza(statistici(), {
      formaGazde: formaEchipa({
        locatie: fereastraForma({
          meciuri: [formaMeci({ acasa: true, golMarcate: 3, golPrimite: 1, adversar: { id: 9, nume: 'Rapid', logo: 'rapid.png' } })],
        }),
        general: fereastraForma({ meciuri: [] }),
      }),
    });
    await tab(user, 'Goluri');
    const rand = screen.getByText('3-1').closest('li') as HTMLElement;
    expect(within(rand).getByRole('img', { name: 'Rapid' })).toBeInTheDocument();
    expect(within(rand).getByTitle('FCSB')).toBeInTheDocument();
  });
});

describe('SectiuneStatistici — „Toate meciurile" (fara regula acasa/deplasare)', () => {
  test('comutatorul de teren anunta alegerea; implicit e regula acasa/deplasare', async () => {
    const user = userEvent.setup();
    const alegeri: string[] = [];
    randeaza(statistici(), { onLocatie: (v) => alegeri.push(v) });
    expect(screen.getByRole('button', { name: 'Acasă / Deplasare' })).toHaveAttribute('aria-pressed', 'true');
    await user.click(screen.getByRole('button', { name: 'Toate meciurile' }));
    expect(alegeri).toEqual(['toate']);
  });

  test('fara onLocatie comutatorul nu apare', () => {
    randeaza(statistici());
    expect(screen.queryByRole('button', { name: 'Toate meciurile' })).not.toBeInTheDocument();
  });

  test('etichetele nu mai spun „acasă" / „în deplasare"', async () => {
    const user = userEvent.setup();
    randeaza(statistici(), { locatie: 'toate', onLocatie: () => {} });
    expect(screen.getAllByText('ultimele 7 meciuri')).toHaveLength(2);
    expect(screen.queryByText('ultimele 7 acasă')).toBeNull();
    expect(screen.getByText(/Ultimele 7 meciuri ale fiecărei echipe, indiferent de teren/)).toBeInTheDocument();
    // fereastra pe locatie ESTE cea generala: fara randuri „(general)" duplicate
    expect(screen.getByText('Goluri marcate (ultimele 7)')).toBeInTheDocument();
    expect(screen.queryByText('Goluri marcate (general)')).toBeNull();

    await tab(user, 'Goluri');
    expect(screen.getAllByText('Ultimele 7 rezultate')).toHaveLength(2);
    expect(screen.queryByText(/Ultimele 7 rezultate acasă/)).toBeNull();

    await tab(user, 'Cornere');
    expect(screen.getAllByText(/\/meci · /).length).toBeGreaterThan(0);
    expect(screen.queryByText(/\/meci acasă/)).toBeNull();
  });

  test('implicit etichetele raman pe teren', async () => {
    const user = userEvent.setup();
    randeaza(statistici());
    expect(screen.getByText('ultimele 7 acasă')).toBeInTheDocument();
    expect(screen.getByText('Goluri marcate (general)')).toBeInTheDocument();
    await tab(user, 'Goluri');
    expect(screen.getByText('Ultimele 7 rezultate acasă')).toBeInTheDocument();
    expect(screen.getByText('Ultimele 7 rezultate în deplasare')).toBeInTheDocument();
  });
});
