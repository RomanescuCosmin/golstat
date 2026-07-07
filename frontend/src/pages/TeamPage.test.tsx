import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getEchipa } from '../api/client';
import { antetEchipa, paginaEchipa } from '../test/factories';
import { randeazaCuRuta } from '../test/rutare';
import { TeamPage } from './TeamPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getEchipa: vi.fn(),
}));

const mockGetEchipa = vi.mocked(getEchipa);

function randeaza(teamId = '33') {
  return randeazaCuRuta(<TeamPage />, { cale: '/echipa/:teamId', ruta: `/echipa/${teamId}` });
}

beforeEach(() => {
  mockGetEchipa.mockReset();
});

describe('TeamPage', () => {
  test('happy path: antet, breadcrumb si blocurile paginii', async () => {
    mockGetEchipa.mockResolvedValue(paginaEchipa());
    randeaza();

    expect((await screen.findAllByText('Manchester United')).length).toBeGreaterThan(0);
    expect(screen.getByText('Următorul meci')).toBeInTheDocument();
    expect(screen.getByText('Poziție în clasament')).toBeInTheDocument();
    expect(mockGetEchipa).toHaveBeenCalledWith(33, undefined, undefined);
  });

  test('fara meci programat: EmptyState in blocul "Următorul meci"', async () => {
    mockGetEchipa.mockResolvedValue(paginaEchipa({ urmatorulMeci: null }));
    randeaza();
    expect(await screen.findByText('Niciun meci programat')).toBeInTheDocument();
  });

  test('schimbarea sezonului recheama API-ul cu sezonul ales', async () => {
    const user = userEvent.setup();
    mockGetEchipa.mockResolvedValue(paginaEchipa());
    randeaza();
    await screen.findAllByText('Manchester United');

    await user.selectOptions(screen.getByRole('combobox'), '2024');
    await waitFor(() => expect(mockGetEchipa).toHaveBeenLastCalledWith(33, undefined, 2024));
  });

  test('navigarea la alta echipa reseteaza sezonul selectat', async () => {
    const user = userEvent.setup();
    mockGetEchipa.mockImplementation(async (teamId) =>
      paginaEchipa({ antet: antetEchipa({ teamId, nume: `Echipa ${teamId}` }) }),
    );

    function SariLaAltaEchipa() {
      const navigate = useNavigate();
      return (
        <button type="button" onClick={() => navigate('/echipa/40')}>
          du-te la 40
        </button>
      );
    }

    render(
      <MemoryRouter initialEntries={['/echipa/33']}>
        <SariLaAltaEchipa />
        <Routes>
          <Route path="/echipa/:teamId" element={<TeamPage />} />
        </Routes>
      </MemoryRouter>,
    );
    await screen.findAllByText('Echipa 33');

    // selecteaza un sezon vechi la echipa 33
    await user.selectOptions(screen.getByRole('combobox'), '2024');
    await waitFor(() => expect(mockGetEchipa).toHaveBeenLastCalledWith(33, undefined, 2024));

    // la echipa 40 sezonul selectat nu se propaga (se cere sezonul implicit)
    await user.click(screen.getByRole('button', { name: 'du-te la 40' }));
    await screen.findAllByText('Echipa 40');
    await waitFor(() => expect(mockGetEchipa).toHaveBeenLastCalledWith(40, undefined, undefined));
  });

  test('404: mesaj dedicat fara retry', async () => {
    mockGetEchipa.mockRejectedValue(new ApiError(404, 'Not Found', 'Nu există.'));
    randeaza();
    expect(await screen.findByText('Echipa nu a fost găsită')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Încearcă din nou' })).toBeNull();
  });

  test('id invalid: eroare fara apel API', async () => {
    randeaza('abc');
    expect(await screen.findByText('Echipă invalidă')).toBeInTheDocument();
    expect(mockGetEchipa).not.toHaveBeenCalled();
  });

  test('nu apare NaN/undefined cu blocuri nule', async () => {
    mockGetEchipa.mockResolvedValue(
      paginaEchipa({ sumar: null, statistici: null, topJucatori: null, statProcente: [], goluriPeInterval: [] }),
    );
    const { container } = randeaza();
    await screen.findAllByText('Manchester United');
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });
});
