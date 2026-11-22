import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import type { MeciCentral, MeciLive } from '../api/types';
import { comutaFavorita } from '../lib/favorite';
import { comutaMeciFavorit } from '../lib/meciuriFavorite';
import { meciCentral } from '../test/factories';
import { FavoritePage } from './FavoritePage';

const getLive = vi.fn<() => Promise<MeciLive[]>>();
const getMatchCenter = vi.fn<(id: number) => Promise<MeciCentral>>();
vi.mock('../api/client', () => ({
  getLive: () => getLive(),
  getMatchCenter: (id: number) => getMatchCenter(id),
}));

function favorit(fixtureId: number, gazde: string, oaspeti: string) {
  comutaMeciFavorit({
    fixtureId,
    kickoff: new Date().toISOString(),
    gazde: { id: fixtureId * 10, nume: gazde, logo: null },
    oaspeti: { id: fixtureId * 10 + 1, nume: oaspeti, logo: null },
    leagueId: 135,
    ligaNume: 'Serie A',
  });
}

function randeaza() {
  return render(
    <MemoryRouter>
      <FavoritePage />
    </MemoryRouter>,
  );
}

describe('FavoritePage', () => {
  beforeEach(() => {
    localStorage.clear();
    getLive.mockReset().mockResolvedValue([]);
    getMatchCenter.mockReset();
  });

  test('fara favorite: mesajele goale pentru meciuri si echipe', () => {
    randeaza();
    expect(screen.getByText('Niciun meci favorit')).toBeInTheDocument();
    expect(screen.getByText('Nicio echipă favorită')).toBeInTheDocument();
  });

  test('meciul favorit live arata minutul si scorul din /live; cel terminat arata Final', async () => {
    favorit(1, 'Parma', 'AS Roma');
    favorit(2, 'Lens', 'Toulouse');
    getMatchCenter.mockImplementation((id) =>
      Promise.resolve(
        id === 1
          ? meciCentral({ fixtureId: 1, inDesfasurare: true, golGazde: 0, golOaspeti: 0, minut: 10 })
          : meciCentral({ fixtureId: 2, inDesfasurare: false, terminat: true, golGazde: 2, golOaspeti: 2, status: 'FT' }),
      ),
    );
    getLive.mockResolvedValue([
      {
        fixtureId: 1, leagueId: 135, ligaNume: 'Serie A', ligaLogo: null,
        gazde: { id: 10, nume: 'Parma', logo: null }, oaspeti: { id: 11, nume: 'AS Roma', logo: null },
        golGazde: 1, golOaspeti: 2, status: '2H', minut: 67,
      },
    ]);

    randeaza();

    const parma = (await screen.findByText('67\'')).closest('li')!;
    expect(within(parma).getByText('1 – 2')).toBeInTheDocument();
    const lens = (await screen.findByText('Final')).closest('li')!;
    expect(within(lens).getByText('Lens')).toBeInTheDocument();
    expect(within(lens).getByText('2 – 2')).toBeInTheDocument();
  });

  test('steluta scoate meciul din favorite; echipele favorite apar si se pot scoate', async () => {
    favorit(1, 'Parma', 'AS Roma');
    comutaFavorita({ id: 65, nume: 'AS Roma', logo: null });
    getMatchCenter.mockReturnValue(new Promise(() => undefined));

    randeaza();

    await userEvent.click(screen.getByRole('button', { name: 'Scoate meciul din favorite' }));
    expect(screen.getByText('Niciun meci favorit')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Scoate AS Roma din favorite' }));
    expect(screen.getByText('Nicio echipă favorită')).toBeInTheDocument();
  });

  test('nu mai afiseaza alegerea sunetului', () => {
    randeaza();
    expect(screen.queryByText('Sunet la gol')).toBeNull();
    expect(screen.queryByRole('radio')).toBeNull();
  });
});
