import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getCompetitie } from '../api/client';
import { meciCompetitie, paginaCompetitie, randClasament } from '../test/factories';
import { randeazaCuRuta } from '../test/rutare';
import { CompetitiePage } from './CompetitiePage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getCompetitie: vi.fn(),
}));

const mockGetCompetitie = vi.mocked(getCompetitie);

function randeaza(leagueId = '39') {
  return randeazaCuRuta(<CompetitiePage />, {
    cale: '/competitie/:leagueId',
    ruta: `/competitie/${leagueId}`,
  });
}

beforeEach(() => {
  mockGetCompetitie.mockReset();
});

describe('CompetitiePage', () => {
  test('liga obisnuita: clasament + topuri + rezultate + program', async () => {
    mockGetCompetitie.mockResolvedValue(paginaCompetitie());
    randeaza();

    expect(await screen.findByRole('heading', { name: 'Premier League' })).toBeInTheDocument();
    expect(screen.getByText('Poziție în clasament')).toBeInTheDocument();
    expect(screen.getByText('Golgheteri')).toBeInTheDocument();
    expect(screen.getByText('M. Rashford')).toBeInTheDocument();
    expect(screen.getByText('Rezultate')).toBeInTheDocument();
    expect(screen.getByText('2 - 1')).toBeInTheDocument();
    // liga obisnuita: fara grupe si fara faze eliminatorii
    expect(screen.queryByText('Faze eliminatorii')).toBeNull();
  });

  test('competitie cu grupe (Cupa Mondiala): grupele inlocuiesc clasamentul, apar eliminatoriile', async () => {
    mockGetCompetitie.mockResolvedValue(
      paginaCompetitie({
        clasament: [],
        grupe: [
          { nume: 'Grupa A', randuri: [randClasament()] },
          { nume: 'Grupa B', randuri: [randClasament({ teamId: 40, nume: 'Liverpool' })] },
        ],
        eliminatorii: [{ runda: 'Final', meciuri: [meciCompetitie()] }],
      }),
    );
    randeaza('1');

    expect(await screen.findByText('Grupa A')).toBeInTheDocument();
    expect(screen.getByText('Grupa B')).toBeInTheDocument();
    expect(screen.getByText('Faze eliminatorii')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Finală' })).toBeInTheDocument();
    expect(screen.queryByText('Poziție în clasament')).toBeNull();
  });

  test('fara clasament si fara grupe: EmptyState dedicat', async () => {
    mockGetCompetitie.mockResolvedValue(paginaCompetitie({ clasament: [], grupe: [] }));
    randeaza();
    expect(await screen.findByText('Clasament indisponibil')).toBeInTheDocument();
  });

  test('404: mesaj dedicat', async () => {
    mockGetCompetitie.mockRejectedValue(new ApiError(404, 'Not Found', 'Nu există.'));
    randeaza();
    expect(await screen.findByText('Competiția nu a fost găsită')).toBeInTheDocument();
  });

  test('id invalid: eroare fara apel API', async () => {
    randeaza('abc');
    expect(await screen.findByText('Competiție invalidă')).toBeInTheDocument();
    expect(mockGetCompetitie).not.toHaveBeenCalled();
  });

  test('nu apare NaN/undefined cu liste goale', async () => {
    mockGetCompetitie.mockResolvedValue(
      paginaCompetitie({ golgheteri: [], pasatori: [], rezultate: [], urmatoare: [] }),
    );
    const { container } = randeaza();
    await screen.findByRole('heading', { name: 'Premier League' });
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });
});
