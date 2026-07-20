import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, getPieteZile } from '../api/client';
import { cotaPiata, meciPiete, pieteZile, ziPiete } from '../test/factories';
import { PietePage } from './PietePage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getPieteZile: vi.fn(),
}));

const mockGet = vi.mocked(getPieteZile);

function randeaza() {
  return render(
    <MemoryRouter>
      <PietePage />
    </MemoryRouter>,
  );
}

/** Trei meciuri intr-o zi cu goluri + cornere, ca sa putem comuta intre piete. */
function dateComplete() {
  return pieteZile({
    zile: [
      ziPiete({
        data: '2026-07-21',
        meciuri: [
          meciPiete({
            fixtureId: 1,
            gazde: { id: 1, nume: 'Slaba', logo: null },
            oaspeti: { id: 2, nume: 'Modesta', logo: null },
            piete: [
              cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.35 }),
              cotaPiata({ piata: 'CORNERE_PESTE', linie: 7.5, probabilitate: 0.9 }),
            ],
          }),
          meciPiete({
            fixtureId: 2,
            gazde: { id: 3, nume: 'Puternica', logo: null },
            oaspeti: { id: 4, nume: 'Ofensiva', logo: null },
            piete: [
              cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.82 }),
              cotaPiata({ piata: 'GOLURI_PESTE', linie: 1.5, probabilitate: 0.95 }),
              cotaPiata({ piata: 'GG', linie: null, probabilitate: 0.7 }),
            ],
          }),
          meciPiete({
            fixtureId: 3,
            gazde: { id: 5, nume: 'Medie', logo: null },
            oaspeti: { id: 6, nume: 'Echilibrata', logo: null },
            piete: [cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: 0.55 })],
          }),
        ],
      }),
    ],
  });
}

describe('PietePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('afiseaza implicit peste 2.5 goluri cu pragul la 30%', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();

    await screen.findByText('Puternica');
    expect(screen.getByText('30%')).toBeInTheDocument();
    // toate cele 3 sunt >= 35% deci trec pragul implicit
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();
  });

  it('sorteaza descrescator in interiorul zilei', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    const { container } = randeaza();

    await screen.findByText('Puternica');
    const procente = Array.from(container.querySelectorAll('a'))
      .map((a) => a.textContent?.match(/(\d+)%/)?.[1])
      .filter(Boolean)
      .map(Number);
    expect(procente).toEqual([82, 55, 35]);
  });

  it('slider-ul de prag filtreaza fara sa refaca cererea', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    // input[type=range] nu raspunde la type(); schimbam valoarea direct
    fireEvent.change(screen.getByRole('slider', { name: 'Șansă minimă' }), { target: { value: '60' } });

    await waitFor(() => expect(screen.queryByText('Slaba')).not.toBeInTheDocument());
    expect(screen.getByText('Puternica')).toBeInTheDocument();
    expect(screen.queryByText('Medie')).not.toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('schimbarea pietei schimba lista si liniile disponibile', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await userEvent.click(screen.getByRole('tab', { name: 'Cornere' }));

    // liniile de goluri dispar, apar cele de cornere
    expect(screen.getByRole('button', { name: '7.5' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '2.5' })).not.toBeInTheDocument();
    // doar meciul 1 are cornere
    expect(screen.getByText('Slaba')).toBeInTheDocument();
    expect(screen.queryByText('Puternica')).not.toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('piata binara ascunde selectorul de linie', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await userEvent.click(screen.getByRole('tab', { name: 'GG' }));

    expect(screen.queryByRole('group', { name: 'Linie' })).not.toBeInTheDocument();
    expect(screen.getByText('Puternica')).toBeInTheDocument();
  });

  it('schimbarea liniei schimba procentele', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await userEvent.click(screen.getByRole('button', { name: '1.5' }));

    expect(screen.getByText('95%')).toBeInTheDocument();
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
  });

  it('prag imposibil da stare goala, nu lista muta', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    fireEvent.change(screen.getByRole('slider', { name: 'Șansă minimă' }), { target: { value: '100' } });

    expect(await screen.findByText('Niciun meci peste prag')).toBeInTheDocument();
    expect(screen.queryByText('Puternica')).not.toBeInTheDocument();
  });

  it('fereastra fara meciuri da stare goala', async () => {
    mockGet.mockResolvedValueOnce(pieteZile({ zile: [] }));
    randeaza();
    expect(await screen.findByText('Niciun meci peste prag')).toBeInTheDocument();
  });

  it('eroarea ofera reincercare care reface cererea', async () => {
    mockGet.mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'Baza e jos.'));
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Încearcă din nou/i }));

    expect(await screen.findByText('Puternica')).toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('nu afiseaza NaN/undefined nici pe date la limita', async () => {
    mockGet.mockResolvedValueOnce(
      pieteZile({
        zile: [
          ziPiete({
            meciuri: [
              meciPiete({
                fixtureId: 9,
                gazde: { id: 9, nume: null, logo: null },
                oaspeti: { id: 10, nume: null, logo: null },
                piete: [cotaPiata({ probabilitate: 0.5, cota: 2, esantion: 1 })],
              }),
            ],
          }),
        ],
      }),
    );
    const { container } = randeaza();

    await screen.findByText(/1 meci peste 30%/);
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });

  it('grupeaza pe zi cu antet propriu', async () => {
    mockGet.mockResolvedValueOnce(
      pieteZile({
        zile: [
          ziPiete({ data: '2026-07-21', meciuri: [meciPiete({ fixtureId: 1 })] }),
          ziPiete({ data: '2026-07-22', meciuri: [meciPiete({ fixtureId: 2 })] }),
        ],
      }),
    );
    const { container } = randeaza();

    await waitFor(() => expect(container.querySelectorAll('section')).toHaveLength(2));
    const sectiuni = container.querySelectorAll('section');
    expect(within(sectiuni[0] as HTMLElement).getByText(/21 iulie 2026/)).toBeInTheDocument();
    expect(within(sectiuni[1] as HTMLElement).getByText(/22 iulie 2026/)).toBeInTheDocument();
  });
});
