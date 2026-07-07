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
  test('zilele au data calendaristica corecta pentru "YYYY-MM-DD" (fara salt UTC)', async () => {
    mockGetProgram.mockResolvedValue(
      program({
        zile: [
          programZi({ data: '2026-07-09' }),
          programZi({ data: '2026-07-10', ligi: [programLiga({ meciuri: [programMeci({ fixtureId: 4002 })] })] }),
        ],
      }),
    );
    randeaza();

    // lock end-to-end pentru bug-ul de fus orar: ziua afisata = ziua din payload
    expect(await screen.findByText(/9 iulie 2026/)).toBeInTheDocument();
    expect(screen.getByText(/10 iulie 2026/)).toBeInTheDocument();
    expect(screen.queryByText(/8 iulie 2026/)).toBeNull();
    expect(mockGetProgram).toHaveBeenCalledWith(7);
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
