import { render, screen } from '@testing-library/react';
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
