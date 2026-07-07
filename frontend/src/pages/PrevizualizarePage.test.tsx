import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getPrevizualizare } from '../api/client';
import { previzualizareMeci } from '../test/factories';
import { randeazaCuRuta } from '../test/rutare';
import { PrevizualizarePage } from './PrevizualizarePage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getPrevizualizare: vi.fn(),
}));
vi.mock('../api/live');

const mockGetPrevizualizare = vi.mocked(getPrevizualizare);

function randeaza(fixtureId = '1001') {
  return randeazaCuRuta(<PrevizualizarePage />, {
    cale: '/meci/:fixtureId',
    ruta: `/meci/${fixtureId}`,
  });
}

beforeEach(() => {
  mockGetPrevizualizare.mockReset();
});

describe('PrevizualizarePage', () => {
  test('happy path: header cu echipele si tabul Prezentare activ implicit', async () => {
    mockGetPrevizualizare.mockResolvedValue(previzualizareMeci());
    randeaza();

    expect(await screen.findByText('Probabilitate rezultat')).toBeInTheDocument();
    expect(screen.getAllByText('Manchester United').length).toBeGreaterThan(0);
    expect(screen.getByRole('tab', { name: /Prezentare/ })).toHaveAttribute('aria-selected', 'true');
    expect(mockGetPrevizualizare).toHaveBeenCalledWith(1001);
  });

  test('taburile comuta continutul (Rezultate → întâlniri directe)', async () => {
    const user = userEvent.setup();
    mockGetPrevizualizare.mockResolvedValue(previzualizareMeci());
    randeaza();
    await screen.findByText('Probabilitate rezultat');

    await user.click(screen.getByRole('tab', { name: /Rezultate/ }));
    expect(screen.queryByText('Probabilitate rezultat')).toBeNull();
    expect(screen.getByText(/Întâlniri directe/i)).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: /Statistici/ }));
    expect(screen.queryByText(/Întâlniri directe/i)).toBeNull();
  });

  test('tab Echipe fara formatii anuntate: EmptyState in loc de teren', async () => {
    const user = userEvent.setup();
    mockGetPrevizualizare.mockResolvedValue(previzualizareMeci({ echipeDeStart: null }));
    randeaza();
    await screen.findByText('Probabilitate rezultat');

    await user.click(screen.getByRole('tab', { name: /Echipe probabile/ }));
    expect(screen.getByText('Formații indisponibile')).toBeInTheDocument();
  });

  test('404: mesaj special cu link spre desfasurarea meciului, fara retry', async () => {
    mockGetPrevizualizare.mockRejectedValue(new ApiError(404, 'Not Found', 'Nu există predicție.'));
    randeaza('1001');

    expect(await screen.findByText('Meciul nu are predicție')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Vezi desfășurarea meciului' })).toHaveAttribute(
      'href',
      '/meci/1001/centru',
    );
    expect(screen.queryByRole('button', { name: 'Încearcă din nou' })).toBeNull();
  });

  test('alta eroare: ErrorState generic cu retry', async () => {
    const user = userEvent.setup();
    mockGetPrevizualizare
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'DB down'))
      .mockResolvedValueOnce(previzualizareMeci());
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));
    expect(await screen.findByText('Probabilitate rezultat')).toBeInTheDocument();
    expect(mockGetPrevizualizare).toHaveBeenCalledTimes(2);
  });

  test('id invalid in ruta: eroare fara apel API', async () => {
    randeaza('abc');
    expect(await screen.findByText('Meci invalid')).toBeInTheDocument();
    expect(mockGetPrevizualizare).not.toHaveBeenCalled();
  });

  test('nu apare NaN/undefined cu statistici partial nule', async () => {
    mockGetPrevizualizare.mockResolvedValue(
      previzualizareMeci({
        statisticiCheie: {
          gazde: { posesieMedie: null, suturiPeMeci: null, suturiPePoarta: null, cornerePeMeci: null, cartonasePeMeci: null },
          oaspeti: { posesieMedie: 45, suturiPeMeci: 11, suturiPePoarta: 4, cornerePeMeci: 5, cartonasePeMeci: 2 },
        },
      }),
    );
    const { container } = randeaza();
    await screen.findByText('Probabilitate rezultat');
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });
});
