import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { cauta } from '../../api/client';
import { rezultatCautare } from '../../test/factories';
import { Cautare } from './Cautare';

vi.mock('../../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/client')>()),
  cauta: vi.fn(),
}));

const mockCauta = vi.mocked(cauta);

function UltimaRuta() {
  const location = useLocation();
  return <p data-testid="ruta">{location.pathname}</p>;
}

function randeaza() {
  return render(
    <MemoryRouter>
      <Cautare />
      <Routes>
        <Route path="*" element={<UltimaRuta />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.useFakeTimers();
  mockCauta.mockReset();
});

afterEach(() => {
  vi.useRealTimers();
});

function scrie(text: string) {
  fireEvent.change(screen.getByRole('combobox'), { target: { value: text } });
}

/** Trece de debounce si lasa promisiunea cautarii sa se aseze in stare. */
async function asteaptaCautarea() {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(300);
  });
}

describe('Cautare (search global)', () => {
  test('sub 2 caractere: niciun apel catre API', async () => {
    randeaza();
    scrie('m');
    await asteaptaCautarea();
    expect(mockCauta).not.toHaveBeenCalled();
  });

  test('debounce 300ms: tastarea rapida produce UN singur apel, cu termenul final', async () => {
    mockCauta.mockResolvedValue([rezultatCautare()]);
    randeaza();

    for (const prefix of ['ma', 'manc', 'manchester']) {
      scrie(prefix);
      await act(async () => {
        await vi.advanceTimersByTimeAsync(100);
      });
    }
    expect(mockCauta).not.toHaveBeenCalled();

    await asteaptaCautarea();
    expect(mockCauta).toHaveBeenCalledTimes(1);
    expect(mockCauta).toHaveBeenCalledWith('manchester', expect.any(AbortSignal));
  });

  test('schimbarea termenului anuleaza cererea precedenta (AbortController)', async () => {
    mockCauta.mockImplementation(() => new Promise(() => {}));
    randeaza();

    scrie('man');
    await asteaptaCautarea();
    const primulSemnal = mockCauta.mock.calls[0]![1] as AbortSignal;
    expect(primulSemnal.aborted).toBe(false);

    scrie('manc');
    expect(primulSemnal.aborted).toBe(true);
  });

  test('rezultatele sunt grupate pe tip, cu antet o singura data per grup', async () => {
    mockCauta.mockResolvedValue([
      rezultatCautare({ tip: 'ECHIPA', id: 33, nume: 'Manchester United' }),
      rezultatCautare({ tip: 'ECHIPA', id: 50, nume: 'Manchester City' }),
      rezultatCautare({ tip: 'JUCATOR', id: 909, nume: 'M. Rashford', subtitlu: 'Manchester United' }),
    ]);
    randeaza();

    scrie('man');
    await asteaptaCautarea();

    expect(screen.getByText('Manchester City')).toBeInTheDocument();
    expect(screen.getAllByText('Echipe')).toHaveLength(1);
    expect(screen.getAllByText('Jucători')).toHaveLength(1);
    expect(screen.getAllByRole('option')).toHaveLength(3);
  });

  test('navigare din tastatura: sagetile muta selectia, Enter deschide pagina', async () => {
    mockCauta.mockResolvedValue([
      rezultatCautare({ tip: 'ECHIPA', id: 33, nume: 'Manchester United' }),
      rezultatCautare({ tip: 'LIGA', id: 39, nume: 'Premier League' }),
    ]);
    randeaza();

    scrie('man');
    await asteaptaCautarea();
    screen.getByText('Premier League');

    const input = screen.getByRole('combobox');
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(screen.getByRole('option', { name: /Premier League/ })).toHaveAttribute('aria-selected', 'true');

    fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('ruta')).toHaveTextContent('/competitie/39');
    // input-ul se goleste si dropdown-ul se inchide dupa selectie
    expect(screen.getByRole('combobox')).toHaveValue('');
    expect(screen.queryByRole('listbox')).toBeNull();
  });

  test('click pe un rezultat navigheaza la pagina lui', async () => {
    mockCauta.mockResolvedValue([rezultatCautare({ tip: 'ECHIPA', id: 33, nume: 'Manchester United' })]);
    randeaza();

    scrie('man');
    await asteaptaCautarea();

    fireEvent.click(screen.getByRole('option', { name: /Manchester United/ }));
    expect(screen.getByTestId('ruta')).toHaveTextContent('/echipa/33');
  });

  test('Escape inchide dropdown-ul fara sa stearga termenul', async () => {
    mockCauta.mockResolvedValue([rezultatCautare()]);
    randeaza();

    scrie('man');
    await asteaptaCautarea();
    screen.getByRole('listbox');

    fireEvent.keyDown(screen.getByRole('combobox'), { key: 'Escape' });
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(screen.getByRole('combobox')).toHaveValue('man');
  });

  test('fara rezultate: mesajul "Niciun rezultat"', async () => {
    mockCauta.mockResolvedValue([]);
    randeaza();

    scrie('xyzq');
    await asteaptaCautarea();
    expect(screen.getByText('Niciun rezultat')).toBeInTheDocument();
  });
});
