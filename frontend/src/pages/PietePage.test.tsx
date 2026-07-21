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

/** Trei meciuri din trei campionate diferite, pentru filtrul de campionat. */
function dateMultiLiga() {
  const meci = (id: number, ligaId: number, ligaNume: string, gazde: string, p: number) =>
    meciPiete({
      fixtureId: id,
      liga: { id: ligaId, nume: ligaNume, logo: null },
      gazde: { id: id * 10, nume: gazde, logo: null },
      oaspeti: { id: id * 10 + 1, nume: `Adversar ${id}`, logo: null },
      piete: [cotaPiata({ piata: 'GOLURI_PESTE', linie: 2.5, probabilitate: p })],
    });
  return pieteZile({
    zile: [
      ziPiete({
        data: '2026-07-21',
        meciuri: [
          meci(1, 39, 'Premier League', 'Arsenal', 0.9),
          meci(2, 140, 'La Liga', 'Barcelona', 0.86),
          meci(3, 283, 'Liga I', 'Dinamo', 0.4),
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

  it('schimbarea tipului schimba lista si optiunile din dropdown', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await userEvent.click(screen.getByRole('tab', { name: 'Cornere' }));

    // liniile de goluri dispar din dropdown, apar cele de cornere
    const selector = screen.getByLabelText('Piața');
    expect(within(selector).getByRole('option', { name: 'Peste 7.5' })).toBeInTheDocument();
    expect(within(selector).queryByRole('option', { name: 'Peste 2.5' })).not.toBeInTheDocument();
    // doar meciul 1 are cornere
    expect(screen.getByText('Slaba')).toBeInTheDocument();
    expect(screen.queryByText('Puternica')).not.toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('piata binara are doar directii in dropdown, fara linii', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await userEvent.click(screen.getByRole('tab', { name: 'GG' }));

    const optiuni = within(screen.getByLabelText('Piața')).getAllByRole('option');
    expect(optiuni.map((o) => o.textContent)).toEqual(['Ambele înscriu', 'Nu ambele']);
    expect(screen.getByText('Puternica')).toBeInTheDocument();
  });

  it('schimbarea liniei din dropdown schimba procentele', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    await userEvent.selectOptions(screen.getByLabelText('Piața'), 'GOLURI_PESTE|1.5');

    expect(screen.getByText('95%')).toBeInTheDocument();
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
  });

  it('fiecare rand arata piata aleasa, cu unitate', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    // eticheta lunga tine minte ce inseamna procentul cand derulezi lista
    expect(screen.getAllByText('Peste 2.5 goluri')).toHaveLength(3);

    await userEvent.click(screen.getByRole('tab', { name: 'Cornere' }));
    expect(screen.getByText('Peste 7.5 cornere')).toBeInTheDocument();
  });

  it('steluta marcheaza meciul si ramane apasata, fara sa navigheze', async () => {
    mockGet.mockResolvedValueOnce(dateComplete());
    randeaza();
    await screen.findByText('Puternica');

    const steluta = screen.getByRole('button', { name: /Salvează Puternica/ });
    expect(steluta).toHaveAttribute('aria-pressed', 'false');

    await userEvent.click(steluta);

    expect(screen.getByRole('button', { name: /Salvează Puternica/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    // lista ramane pe loc: marcajul nu e un filtru
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();
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

  it('filtreaza pe campionat, cu selectii multiple, fara refetch', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    // implicit: toate campionatele
    expect(screen.getByRole('button', { name: /Toate/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Premier League/ }));
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
    expect(screen.getByText('Arsenal')).toBeInTheDocument();
    expect(screen.queryByText('Barcelona')).not.toBeInTheDocument();

    // a doua selectie se ADAUGA
    await userEvent.click(screen.getByRole('button', { name: /La Liga/ }));
    expect(screen.getByText(/2 meciuri peste 30%/)).toBeInTheDocument();
    expect(screen.getByText('Arsenal')).toBeInTheDocument();
    expect(screen.getByText('Barcelona')).toBeInTheDocument();
    expect(screen.queryByText('Dinamo')).not.toBeInTheDocument();

    // deselectarea scoate campionatul
    await userEvent.click(screen.getByRole('button', { name: /Premier League/ }));
    expect(screen.getByText(/1 meci peste 30%/)).toBeInTheDocument();
    expect(screen.queryByText('Arsenal')).not.toBeInTheDocument();

    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('„Toate" reseteaza selectia de campionate', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await userEvent.click(screen.getByRole('button', { name: /Premier League/ }));
    expect(screen.getByRole('button', { name: /Toate/ })).toHaveAttribute('aria-pressed', 'false');

    await userEvent.click(screen.getByRole('button', { name: /Toate/ }));
    expect(screen.getByText(/3 meciuri peste 30%/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Toate/ })).toHaveAttribute('aria-pressed', 'true');
  });

  it('selectia de campionat se pastreaza cand schimbi piata sau pragul', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    await userEvent.click(screen.getByRole('button', { name: /Premier League/ }));
    fireEvent.change(screen.getByRole('slider', { name: 'Șansă minimă' }), { target: { value: '10' } });

    expect(screen.getByRole('button', { name: /Premier League/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.queryByText('Barcelona')).not.toBeInTheDocument();
  });

  it('campionatul fara meciuri la pragul curent ramane in lista, cu 0', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    fireEvent.change(screen.getByRole('slider', { name: 'Șansă minimă' }), { target: { value: '85' } });

    // Liga I (0.4) cade sub prag, dar pastila ramane — altfel lista ar sari sub deget
    const pastila = await screen.findByRole('button', { name: /Liga I/ });
    expect(pastila).toHaveTextContent('0');
  });

  it('selectand un campionat gol se ajunge la stare goala explicita', async () => {
    mockGet.mockResolvedValueOnce(dateMultiLiga());
    randeaza();
    await screen.findByText('Arsenal');

    fireEvent.change(screen.getByRole('slider', { name: 'Șansă minimă' }), { target: { value: '85' } });
    await userEvent.click(await screen.findByRole('button', { name: /Liga I/ }));

    expect(await screen.findByText('Niciun meci peste prag')).toBeInTheDocument();
    expect(screen.getByText(/campionatele alese/)).toBeInTheDocument();
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
