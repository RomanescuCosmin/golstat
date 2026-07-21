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

const piataGoala = (linii: LinieStatDto[]): PiataStatDto => ({
  linii,
  gazde: medii(null, null),
  oaspeti: medii(null, null),
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
};

function randeaza(s: StatisticiAvansateDto) {
  return render(<SectiuneStatistici statistici={s} gazde={GAZDE} oaspeti={OASPETI} />);
}

/** Bara de probabilitate custom se recunoaste dupa umplerea cu gradient. */
const bara = (container: HTMLElement) => container.querySelector('.bg-gradient-to-r');

/* ──────────────── teste ──────────────── */

describe('SectiuneStatistici — tabul Goluri (implicit)', () => {
  test('afiseaza linia, procentul modelat, frecventele si legenda completa', () => {
    const { container } = randeaza(statistici());
    expect(screen.getByText('Peste 2.5')).toBeInTheDocument();
    expect(screen.getByText('62%')).toBeInTheDocument();
    expect(screen.getByText('5/7')).toBeInTheDocument();
    expect(screen.getByText('3/7')).toBeInTheDocument();
    expect(container.textContent).toContain(
      'FCSB: peste 2.5 goluri în 5/7 meciuri acasă (4/7 în general)',
    );
    expect(container.textContent).toContain('CFR Cluj: 3/7 în deplasare (2/7 în general)');
    // bara de probabilitate e prezenta si umpluta la procentul modelat
    expect(bara(container)).not.toBeNull();
    expect((bara(container) as HTMLElement).style.width).toBe('62%');
  });

  test('mediile pe meci apar formatate, cu echipa si locatia', () => {
    const { container } = randeaza(statistici());
    expect(container.textContent).toContain('FCSB');
    expect(container.textContent).toContain('(acasă): 1,9 goluri/meci · total meci 3,1');
    expect(container.textContent).toContain('general 1,7/meci');
    expect(container.textContent).toContain('(deplasare): 1,2 goluri/meci · total meci 2,6');
  });

  test('medie proprie lipsa: afiseaza "—", nu text gol', () => {
    const s = statistici({
      goluri: { linii: [linie(2.5, 0.62)], gazde: medii(null, 2.5), oaspeti: medii(1.2, 2.6) },
    });
    const { container } = randeaza(s);
    expect(container.textContent).toContain('(acasă): — goluri/meci · total meci 2,5');
  });
});

describe('SectiuneStatistici — fara istoric (frecvente 0/0)', () => {
  const FARA_ISTORIC = statistici({ goluri: piataGoala([linieFaraIstoric(2.5, 0.55)]) });

  test('punctele de frecventa sunt inlocuite cu "fără date"', () => {
    randeaza(FARA_ISTORIC);
    expect(screen.getAllByText('fără date')).toHaveLength(2);
    expect(screen.queryByText('0/0')).toBeNull();
  });

  test('legenda explica sursa probabilitatii', () => {
    randeaza(FARA_ISTORIC);
    expect(
      screen.getByText('Fără istoric — probabilitatea vine din media ligii.'),
    ).toBeInTheDocument();
  });

  test('bara de progres NU apare cand nu exista date', () => {
    const { container } = randeaza(FARA_ISTORIC);
    expect(bara(container)).toBeNull();
  });

  test('procentul modelat NU se afiseaza — doar "—"', () => {
    // Fara istoric, 55% ar veni exclusiv din media ligii — un procent "sigur" fara niciun meci
    // masurat in spate. Nu-l afisam.
    randeaza(FARA_ISTORIC);
    expect(screen.queryByText('55%')).toBeNull();
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  test('cornere fara istoric la meci terminat: fara procent si fara verdict ✓/✗', async () => {
    const user = userEvent.setup();
    const s = statistici({
      cornere: piataGoala([linieFaraIstoric(9.5, 0.86)]),
      rezultat: REZULTAT,
    });
    const { container } = randeaza(s);
    await user.click(screen.getByRole('tab', { name: 'Cornere' }));

    expect(screen.queryByText('86%')).toBeNull();
    expect(screen.getByText('—')).toBeInTheDocument();
    // fara model cu date reale nu exista verdict, desi meciul are total real de cornere
    expect(container.textContent).not.toContain('✓');
    expect(container.textContent).not.toContain('✗');
  });
});

describe('SectiuneStatistici — meci terminat (badge ✓/✗)', () => {
  test('modelul a nimerit: total real 3 > linia 2.5 favorizata → "✓ Peste"', () => {
    randeaza(statistici({ rezultat: REZULTAT }));
    expect(screen.getByText('Rezultat real: 3 goluri')).toBeInTheDocument();
    expect(screen.getByText('✓ Peste')).toBeInTheDocument();
  });

  test('modelul a ratat: total real 1 < linia 2.5 favorizata → "✗ Sub"', () => {
    randeaza(statistici({ rezultat: { ...REZULTAT, totalGoluri: 1 } }));
    expect(screen.getByText('✗ Sub')).toBeInTheDocument();
  });

  test('meci viitor (rezultat null): niciun badge si niciun "Rezultat real"', () => {
    const { container } = randeaza(statistici());
    expect(container.textContent).not.toContain('✓');
    expect(container.textContent).not.toContain('✗');
    expect(container.textContent).not.toContain('Rezultat real');
  });
});

describe('SectiuneStatistici — meci terminat fara statistica (furnizor fara acoperire)', () => {
  // Cazul real: Liga I 2026 raporta statistics_fixtures=false, deci faulturile/cornerele lipseau
  // desi meciul se jucase. Inainte nu se randa NIMIC, deci arata identic cu un meci neinceput.
  test('tabul Faulturi: stare neutra explicita, nu verdict si nu gol', async () => {
    const user = userEvent.setup();
    const { container } = randeaza(statistici({ rezultat: REZULTAT }));
    await user.click(screen.getByRole('tab', { name: 'Faulturi' }));

    expect(screen.getByText('Fără statistici la acest meci')).toBeInTheDocument();
    expect(screen.getAllByText('– fără date').length).toBeGreaterThan(0);
    // nu inventam un verdict din lipsa de date
    expect(container.textContent).not.toContain('✓');
    expect(container.textContent).not.toContain('✗');
    expect(container.textContent).not.toContain('Rezultat real');
  });

  test('meci viitor: NU apare starea neutra (lipsa e asteptata)', () => {
    const { container } = randeaza(statistici());
    expect(container.textContent).not.toContain('Fără statistici la acest meci');
    expect(screen.queryByText('– fără date')).toBeNull();
  });

  test('statistica prezenta: verdict normal, fara stare neutra', async () => {
    const user = userEvent.setup();
    const { container } = randeaza(statistici({ rezultat: REZULTAT }));
    await user.click(screen.getByRole('tab', { name: 'Cornere' }));

    expect(screen.getByText('Rezultat real: 9 cornere')).toBeInTheDocument();
    expect(container.textContent).not.toContain('Fără statistici la acest meci');
    expect(screen.queryByText('– fără date')).toBeNull();
  });
});

describe('SectiuneStatistici — taburi', () => {
  test('comutarea pe Cornere afiseaza piata de cornere', async () => {
    const user = userEvent.setup();
    randeaza(statistici({ rezultat: REZULTAT }));
    await user.click(screen.getByRole('tab', { name: 'Cornere' }));
    expect(screen.getByText('Peste 9.5')).toBeInTheDocument();
    expect(screen.getByText('Rezultat real: 9 cornere')).toBeInTheDocument();
  });

  test('tabul GG afiseaza probabilitatea si legenda marcat/primit', async () => {
    const user = userEvent.setup();
    const { container } = randeaza(statistici());
    await user.click(screen.getByRole('tab', { name: 'Ambele marchează' }));
    expect(screen.getByText('58%')).toBeInTheDocument();
    expect(container.textContent).toContain(
      'FCSB: a marcat în 6/7 și a primit în 4/7 meciuri acasă',
    );
    expect(container.textContent).toContain(
      'CFR Cluj: a marcat în 5/7 și a primit în 3/7 în deplasare',
    );
  });

  test('egaluri si reprize lipsa: tabul afiseaza EmptyState, nu card gol', async () => {
    const user = userEvent.setup();
    randeaza(statistici({ egaluri: null, reprize: null }));
    await user.click(screen.getByRole('tab', { name: 'Pauză / final egal' }));
    expect(screen.getByText('Fără date')).toBeInTheDocument();
    expect(
      screen.getByText('Nu există istoric de reprize pentru aceste echipe.'),
    ).toBeInTheDocument();
  });

  test('niciun tab nu afiseaza NaN / undefined / text gol la procente', async () => {
    const user = userEvent.setup();
    const { container } = randeaza(statistici({ rezultat: REZULTAT }));
    for (const tab of screen.getAllByRole('tab')) {
      await user.click(tab);
      expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
    }
  });
});

describe('SectiuneStatistici — robustete la date defecte', () => {
  test('probabilitate lipsa din JSON: nu se afiseaza "NaN%" utilizatorului', () => {
    const stricat = statistici({
      goluri: piata([
        { ...linie(2.5, 0.62), probabilitate: undefined as unknown as number },
      ]),
    });
    const { container } = randeaza(stricat);
    expect(container.textContent).not.toContain('NaN');
  });
});
