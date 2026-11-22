import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import type { MeciZiGrupat } from '../../api/types';
import { formatOra } from '../../lib/format';
import { RandMeciPremium } from './RandMeciPremium';
import userEvent from '@testing-library/user-event';
import { meciuriFavorite } from '../../lib/meciuriFavorite';

function meci(over: Partial<MeciZiGrupat> = {}): MeciZiGrupat {
  return {
    fixtureId: 101,
    kickoff: '2026-07-07T19:00:00+03:00',
    gazde: { id: 1, nume: 'FCSB', logo: null },
    oaspeti: { id: 2, nume: 'Rapid', logo: null },
    golGazde: null,
    golOaspeti: null,
    status: 'NS',
    inDesfasurare: false,
    terminat: false,
    minut: null,
    runda: null,
    predictie: null,
    ...over,
  };
}

function randeaza(m: MeciZiGrupat) {
  return render(
    <MemoryRouter>
      <RandMeciPremium meci={m} />
    </MemoryRouter>,
  );
}

describe('RandMeciPremium', () => {
  test('meci viitor: ora, echipele si scorul gol "–", fara bara de probabilitate', () => {
    const m = meci();
    const { container } = randeaza(m);
    expect(screen.getByText(formatOra(m.kickoff))).toBeInTheDocument();
    expect(screen.getByText('FCSB')).toBeInTheDocument();
    expect(screen.getByText('Rapid')).toBeInTheDocument();
    expect(screen.getByText('–')).toBeInTheDocument();
    // predictie null → nicio bara
    expect(container.querySelector('.h-2')).toBeNull();
  });

  test('echipa fara nume in DB: fallback "Echipa #id", niciodata text gol', () => {
    randeaza(meci({ gazde: { id: 93, nume: null, logo: null } }));
    expect(screen.getByText('Echipa #93')).toBeInTheDocument();
  });

  test('meci live: minutul si scorul curent', () => {
    randeaza(meci({ inDesfasurare: true, status: '1H', minut: 37, golGazde: 1, golOaspeti: 0 }));
    expect(screen.getByText("37'")).toBeInTheDocument();
    expect(screen.getByText('1 – 0')).toBeInTheDocument();
  });

  test('meci live la pauza: "Pauză" in loc de minut', () => {
    randeaza(meci({ inDesfasurare: true, status: 'HT', minut: 45, golGazde: 0, golOaspeti: 0 }));
    expect(screen.getByText('Pauză')).toBeInTheDocument();
  });

  test('meci live fara minut colectat: fallback "LIVE"', () => {
    randeaza(meci({ inDesfasurare: true, status: '1H', minut: null, golGazde: 0, golOaspeti: 0 }));
    expect(screen.getByText('LIVE')).toBeInTheDocument();
  });

  test('meci live cu goluri inca necolectate: scor "0 – 0", nu "–"', () => {
    // imediat dupa kickoff golurile pot fi null in DB; la un meci LIVE scorul e prin definitie 0-0
    randeaza(meci({ inDesfasurare: true, status: '1H', minut: 2, golGazde: null, golOaspeti: null }));
    expect(screen.getByText('0 – 0')).toBeInTheDocument();
  });

  test('meci terminat: "Final" si scorul', () => {
    randeaza(meci({ terminat: true, status: 'FT', golGazde: 2, golOaspeti: 1 }));
    expect(screen.getByText('Final')).toBeInTheDocument();
    expect(screen.getByText('2 – 1')).toBeInTheDocument();
  });

  test('cu predictie: bara 1X2 apare cu procentele ei', () => {
    const { container } = randeaza(
      meci({
        predictie: {
          gazde: { procent: 50, cota: 2 },
          egal: { procent: 30, cota: 3.33 },
          oaspeti: { procent: 20, cota: 5 },
        },
      }),
    );
    expect(container.querySelector('.h-2')).not.toBeNull();
    expect(screen.getByText('50%')).toBeInTheDocument();
    expect(screen.getByText('30%')).toBeInTheDocument();
    expect(screen.getByText('20%')).toBeInTheDocument();
  });
});

describe('RandMeciPremium – meci favorit', () => {
  test('steluta de meci salveaza meciul (cu liga) in favorite si o a doua apasare il scoate', async () => {
    localStorage.clear();
    render(
      <MemoryRouter>
        <RandMeciPremium meci={meci({ kickoff: new Date().toISOString() })} liga={{ leagueId: 283, nume: 'Liga 1' }} />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Adaugă meciul la favorite' }));

    expect(screen.getByRole('button', { name: 'Scoate meciul din favorite' })).toHaveAttribute('aria-pressed', 'true');
    const salvate = meciuriFavorite();
    expect(salvate).toHaveLength(1);
    expect(salvate[0]).toMatchObject({ fixtureId: 101, leagueId: 283, ligaNume: 'Liga 1' });

    await userEvent.click(screen.getByRole('button', { name: 'Scoate meciul din favorite' }));
    expect(meciuriFavorite()).toHaveLength(0);
  });
});
