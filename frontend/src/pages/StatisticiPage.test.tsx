import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getStatisticiLigi } from '../api/client';
import type { StatisticiLiga } from '../api/types';
import { StatisticiPage } from './StatisticiPage';

// echivalentul @Mock: doar functia de fetch e inlocuita, ApiError ramane clasa reala
vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getStatisticiLigi: vi.fn(),
}));

const mockGetStatisticiLigi = vi.mocked(getStatisticiLigi);

function liga(over: Partial<StatisticiLiga> = {}): StatisticiLiga {
  return {
    leagueId: 39,
    nume: 'Premier League',
    tara: 'Anglia',
    logo: null,
    sezon: 2025,
    medieGoluri: 2.8,
    medieCornere: 10.2,
    medieFaulturi: 21.5,
    medieCartonase: 4.1,
    ...over,
  };
}

function randeaza() {
  return render(
    <MemoryRouter>
      <StatisticiPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mockGetStatisticiLigi.mockReset();
});

describe('StatisticiPage', () => {
  test('afiseaza tabelul cu ligile si mediile formatate', async () => {
    mockGetStatisticiLigi.mockResolvedValue([
      liga(),
      liga({ leagueId: 140, nume: 'La Liga', tara: 'Spania', medieGoluri: 2.55 }),
    ]);
    randeaza();

    expect(await screen.findByText('Premier League')).toBeInTheDocument();
    expect(screen.getByText('La Liga')).toBeInTheDocument();
    expect(screen.getByText('2,80')).toBeInTheDocument();
    expect(screen.getByText('2,55')).toBeInTheDocument();
    expect(screen.getAllByText('2025/26')).toHaveLength(2);
    expect(mockGetStatisticiLigi).toHaveBeenCalledTimes(1);
  });

  test('liga fara nume in DB: fallback "Liga #id"', async () => {
    mockGetStatisticiLigi.mockResolvedValue([liga({ leagueId: 999, nume: null, tara: null })]);
    randeaza();
    expect(await screen.findByText('Liga #999')).toBeInTheDocument();
  });

  test('medie necolectata (null): celula afiseaza "–" si NU deseneaza bara', async () => {
    mockGetStatisticiLigi.mockResolvedValue([liga({ medieGoluri: null })]);
    randeaza();
    await screen.findByText('Premier League');

    const rand = screen.getByRole('row', { name: /Premier League/ });
    const celulaGoluri = within(rand).getAllByRole('cell')[1];
    expect(celulaGoluri).toHaveTextContent('–');
    // fara valoare nu exista umplere (singurul element cu width inline e umplerea barei)
    expect(celulaGoluri.querySelector('[style]')).toBeNull();
  });

  test('medie exact 0: bara e goala, nu o umplere falsa', async () => {
    mockGetStatisticiLigi.mockResolvedValue([
      liga(),
      liga({ leagueId: 283, nume: 'Liga 1', tara: 'România', medieGoluri: 0 }),
    ]);
    randeaza();
    await screen.findByText('Liga 1');

    const rand = screen.getByRole('row', { name: /Liga 1/ });
    const celulaGoluri = within(rand).getAllByRole('cell')[1];
    const umplere = celulaGoluri.querySelector<HTMLElement>('[style]');
    expect(umplere?.style.width ?? '0%').toBe('0%');
  });

  test('lista goala: EmptyState in loc de tabel gol', async () => {
    mockGetStatisticiLigi.mockResolvedValue([]);
    randeaza();
    expect(await screen.findByText('Nu există încă statistici pe ligă')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });

  test('eroare API: ErrorState cu detaliul erorii, iar retry recheama API-ul', async () => {
    const user = userEvent.setup();
    mockGetStatisticiLigi
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'Baza de date nu răspunde.'))
      .mockResolvedValueOnce([liga()]);
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    expect(screen.getByText('Baza de date nu răspunde.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));

    expect(await screen.findByText('Premier League')).toBeInTheDocument();
    // echivalentul verify(mock, times(2))
    await waitFor(() => expect(mockGetStatisticiLigi).toHaveBeenCalledTimes(2));
  });

  test('nicio celula nu ramane goala si nu apare NaN in tabel', async () => {
    mockGetStatisticiLigi.mockResolvedValue([
      liga({ medieGoluri: null, medieCornere: 0, medieFaulturi: 21.5, medieCartonase: null }),
    ]);
    const { container } = randeaza();
    await screen.findByText('Premier League');

    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
    const rand = screen.getByRole('row', { name: /Premier League/ });
    for (const celula of within(rand).getAllByRole('cell').slice(1, 5)) {
      expect(celula.textContent?.trim()).not.toBe('');
    }
  });
});
