import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getProgram } from '../api/client';
import { program, programLiga, programMeci, programZi } from '../test/factories';
import { ProgramPage } from './ProgramPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getProgram: vi.fn(),
}));

const mockGetProgram = vi.mocked(getProgram);

function randeaza() {
  return render(
    <MemoryRouter>
      <ProgramPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mockGetProgram.mockReset();
});

describe('ProgramPage', () => {
  function programPeDouaZile() {
    return program({
      zile: [
        programZi({ data: '2026-07-09' }),
        programZi({ data: '2026-07-10', ligi: [programLiga({ meciuri: [programMeci({ fixtureId: 4002 })] })] }),
      ],
    });
  }

  test('zilele au data calendaristica corecta pentru "YYYY-MM-DD" (fara salt UTC)', async () => {
    const user = userEvent.setup();
    mockGetProgram.mockResolvedValue(programPeDouaZile());
    randeaza();

    await user.click(await screen.findByRole('button', { name: /Toate zilele/ }));

    // lock end-to-end pentru bug-ul de fus orar: ziua afisata = ziua din payload
    expect(screen.getByText(/9 iulie 2026/)).toBeInTheDocument();
    expect(screen.getByText(/10 iulie 2026/)).toBeInTheDocument();
    expect(screen.queryByText(/8 iulie 2026/)).toBeNull();
    expect(mockGetProgram).toHaveBeenCalledWith(7);
  });

  test('banda de zile: prima zi e preselectata, restul sunt ascunse', async () => {
    mockGetProgram.mockResolvedValue(programPeDouaZile());
    randeaza();

    expect(await screen.findByText(/9 iulie 2026/)).toBeInTheDocument();
    expect(screen.queryByText(/10 iulie 2026/)).toBeNull();
  });

  test('banda de zile: click pe alta zi schimba lista, "Toate zilele" le arata pe toate', async () => {
    const user = userEvent.setup();
    mockGetProgram.mockResolvedValue(programPeDouaZile());
    randeaza();

    await user.click(await screen.findByRole('button', { name: /10 iul/ }));
    expect(screen.getByText(/10 iulie 2026/)).toBeInTheDocument();
    expect(screen.queryByText(/9 iulie 2026/)).toBeNull();

    await user.click(screen.getByRole('button', { name: /Toate zilele/ }));
    expect(screen.getByText(/9 iulie 2026/)).toBeInTheDocument();
    expect(screen.getByText(/10 iulie 2026/)).toBeInTheDocument();
  });

  test('banda de zile arata numarul de meciuri al fiecarei zile', async () => {
    mockGetProgram.mockResolvedValue(
      program({ zile: [programZi({ ligi: [programLiga({ meciuri: [programMeci(), programMeci({ fixtureId: 4003 })] })] })] }),
    );
    randeaza();

    expect(await screen.findByRole('button', { name: /2 meciuri/ })).toBeInTheDocument();
  });

  test('program gol: EmptyState', async () => {
    mockGetProgram.mockResolvedValue(program({ zile: [] }));
    randeaza();
    expect(await screen.findByText('Niciun meci programat')).toBeInTheDocument();
  });

  test('eroare API: ErrorState cu retry', async () => {
    const user = userEvent.setup();
    mockGetProgram
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'DB down'))
      .mockResolvedValueOnce(program());
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));
    expect(await screen.findByText(/9 iulie 2026/)).toBeInTheDocument();
    expect(mockGetProgram).toHaveBeenCalledTimes(2);
  });
});
