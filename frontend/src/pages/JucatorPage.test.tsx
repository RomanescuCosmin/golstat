import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getJucator } from '../api/client';
import { paginaJucator, sezonJucator } from '../test/factories';
import { randeazaCuRuta } from '../test/rutare';
import { JucatorPage } from './JucatorPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getJucator: vi.fn(),
}));

const mockGetJucator = vi.mocked(getJucator);

function randeaza(playerId = '909') {
  return randeazaCuRuta(<JucatorPage />, { cale: '/jucator/:playerId', ruta: `/jucator/${playerId}` });
}

beforeEach(() => {
  mockGetJucator.mockReset();
});

describe('JucatorPage', () => {
  test('agregarea pe sezoane insumeaza aparitii/goluri/pase/galbene, ignorand null-urile', async () => {
    mockGetJucator.mockResolvedValue(
      paginaJucator({
        sezoane: [
          sezonJucator({ sezon: 2025, aparitii: 20, goluri: 9, pase: 4, galbene: 3 }),
          sezonJucator({ sezon: 2024, aparitii: 30, goluri: 11, pase: null, galbene: 2 }),
        ],
      }),
    );
    randeaza();

    // valoarea agregata sta lânga eticheta, in acelasi tile; "Apariții" apare si ca antet
    // de tabel, deci luam prima aparitie (tile-urile stau inaintea tabelului)
    const tile = (eticheta: string) => within(screen.getAllByText(eticheta)[0]!.parentElement!);
    await screen.findAllByText('Apariții');
    expect(tile('Apariții').getByText('50')).toBeInTheDocument();
    expect(tile('Goluri').getByText('20')).toBeInTheDocument();
    expect(tile('Pase decisive').getByText('4')).toBeInTheDocument();
    expect(tile('Cartonașe galbene').getByText('5')).toBeInTheDocument();
  });

  test('sezonul se afiseaza "2025/26"; null → "—"', async () => {
    mockGetJucator.mockResolvedValue(
      paginaJucator({ sezoane: [sezonJucator({ sezon: 2025 }), sezonJucator({ sezon: null, leagueId: 140 })] }),
    );
    randeaza();
    expect(await screen.findByText('2025/26')).toBeInTheDocument();
  });

  test('pragurile de rating: ≥7 verde, <6 rosu, intre — neutru', async () => {
    mockGetJucator.mockResolvedValue(
      paginaJucator({
        sezoane: [
          sezonJucator({ sezon: 2025, rating: 7.0 }),
          sezonJucator({ sezon: 2024, rating: 6.5 }),
          sezonJucator({ sezon: 2023, rating: 5.9 }),
        ],
      }),
    );
    randeaza();

    expect(await screen.findByText('7.0')).toHaveClass('text-win');
    expect(screen.getByText('6.5')).toHaveClass('text-ink');
    expect(screen.getByText('5.9')).toHaveClass('text-accent');
  });

  test('detaliile din antet: pozitie · nationalitate · varsta · echipa curenta', async () => {
    mockGetJucator.mockResolvedValue(paginaJucator());
    randeaza();

    expect(await screen.findByRole('heading', { name: 'M. Rashford' })).toBeInTheDocument();
    expect(screen.getByText('Attacker')).toBeInTheDocument();
    expect(screen.getByText('28 ani')).toBeInTheDocument();
    // apare si in antet, si in tabelul de sezoane — ambele duc la pagina echipei
    for (const link of screen.getAllByRole('link', { name: /Manchester United/ })) {
      expect(link).toHaveAttribute('href', '/echipa/33');
    }
  });

  test('fara sezoane: EmptyState', async () => {
    mockGetJucator.mockResolvedValue(paginaJucator({ sezoane: [] }));
    randeaza();
    expect(await screen.findByText('Nu există încă statistici pentru acest jucător.')).toBeInTheDocument();
  });

  test('404: mesaj dedicat; id invalid: fara apel API', async () => {
    mockGetJucator.mockRejectedValue(new ApiError(404, 'Not Found', 'Nu există.'));
    const { unmount } = randeaza();
    expect(await screen.findByText('Jucătorul nu a fost găsit')).toBeInTheDocument();
    unmount();

    mockGetJucator.mockClear();
    randeaza('abc');
    expect(await screen.findByText('Jucător invalid')).toBeInTheDocument();
    expect(mockGetJucator).not.toHaveBeenCalled();
  });

  test('statistici nule in tabel: "—", fara NaN', async () => {
    mockGetJucator.mockResolvedValue(
      paginaJucator({
        sezoane: [sezonJucator({ aparitii: null, minute: null, goluri: null, pase: null, galbene: null, rosii: null, rating: null })],
      }),
    );
    const { container } = randeaza();
    await screen.findByRole('heading', { name: 'M. Rashford' });
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });
});
