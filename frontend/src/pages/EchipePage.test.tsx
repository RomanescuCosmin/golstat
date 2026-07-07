import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getCompetitie } from '../api/client';
import { paginaCompetitie, randClasament } from '../test/factories';
import { EchipePage } from './EchipePage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getCompetitie: vi.fn(),
}));

const mockGetCompetitie = vi.mocked(getCompetitie);

function randeaza() {
  return render(
    <MemoryRouter>
      <EchipePage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mockGetCompetitie.mockReset();
});

describe('EchipePage', () => {
  test('gridul echipelor din clasament, cu puncte si meciuri jucate', async () => {
    mockGetCompetitie.mockResolvedValue(paginaCompetitie());
    randeaza();

    expect(await screen.findByRole('link', { name: /Manchester United/ })).toHaveAttribute('href', '/echipa/33');
    expect(screen.getByText('26 pct · 12 MJ')).toBeInTheDocument();
    expect(mockGetCompetitie).toHaveBeenCalledWith(39);
  });

  test('valori lipsa: "—" in loc de goluri de afisare', async () => {
    mockGetCompetitie.mockResolvedValue(
      paginaCompetitie({ clasament: [randClasament({ rank: null, puncte: null, jucate: null })] }),
    );
    const { container } = randeaza();
    await screen.findByRole('link', { name: /Manchester United/ });
    expect(screen.getByText('— pct · — MJ')).toBeInTheDocument();
    expect(container.textContent).not.toMatch(/NaN|undefined/);
  });

  test('clasament gol: EmptyState', async () => {
    mockGetCompetitie.mockResolvedValue(paginaCompetitie({ clasament: [] }));
    randeaza();
    expect(await screen.findByText('Nu există echipe de afișat')).toBeInTheDocument();
  });

  test('eroare + retry', async () => {
    const user = userEvent.setup();
    mockGetCompetitie
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'DB down'))
      .mockResolvedValueOnce(paginaCompetitie());
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));
    expect(await screen.findByRole('link', { name: /Manchester United/ })).toBeInTheDocument();
  });

  test('schimbarea ligii din selector recheama API-ul', async () => {
    const user = userEvent.setup();
    mockGetCompetitie.mockResolvedValue(paginaCompetitie());
    randeaza();
    await screen.findByRole('link', { name: /Manchester United/ });

    await user.selectOptions(screen.getByRole('combobox'), '140');
    expect(mockGetCompetitie).toHaveBeenLastCalledWith(140);
  });
});
