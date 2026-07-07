import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getCompetitie } from '../api/client';
import { paginaCompetitie } from '../test/factories';
import { JucatoriPage } from './JucatoriPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getCompetitie: vi.fn(),
}));

const mockGetCompetitie = vi.mocked(getCompetitie);

function randeaza() {
  return render(
    <MemoryRouter>
      <JucatoriPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mockGetCompetitie.mockReset();
});

describe('JucatoriPage', () => {
  test('topurile de golgheteri si pasatori, cu link spre pagina jucatorului', async () => {
    mockGetCompetitie.mockResolvedValue(paginaCompetitie());
    randeaza();

    expect(await screen.findByText('Golgheteri')).toBeInTheDocument();
    expect(screen.getByText('Pase decisive')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /M\. Rashford/ })).toHaveAttribute('href', '/jucator/909');
    expect(screen.getByRole('link', { name: /B\. Fernandes/ })).toHaveAttribute('href', '/jucator/910');
  });

  test('topuri goale: EmptyState in fiecare card', async () => {
    mockGetCompetitie.mockResolvedValue(paginaCompetitie({ golgheteri: [], pasatori: [] }));
    randeaza();
    expect(await screen.findAllByText('Statisticile jucătorilor nu sunt încă disponibile (se colectează)')).toHaveLength(2);
  });

  test('eroare + retry', async () => {
    const user = userEvent.setup();
    mockGetCompetitie
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'DB down'))
      .mockResolvedValueOnce(paginaCompetitie());
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));
    expect(await screen.findByText('Golgheteri')).toBeInTheDocument();
    expect(mockGetCompetitie).toHaveBeenCalledTimes(2);
  });
});
