import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getLive, getMeciuriZi } from '../api/client';
import { ligaZi, meciZi, programZiGrupat } from '../test/factories';
import { MeciuriPage } from './MeciuriPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getMeciuriZi: vi.fn(),
  getLive: vi.fn(),
}));
vi.mock('../api/live');

const mockGetMeciuriZi = vi.mocked(getMeciuriZi);
const mockGetLive = vi.mocked(getLive);

function randeaza() {
  return render(
    <MemoryRouter>
      <MeciuriPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mockGetMeciuriZi.mockReset();
  mockGetLive.mockReset();
  mockGetLive.mockResolvedValue([]);
});

describe('MeciuriPage', () => {
  test('afiseaza competitiile zilei cu meciurile lor', async () => {
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({
        ligi: [
          ligaZi({ leagueId: 901, nume: 'Liga Alfa', meciuri: [meciZi({ fixtureId: 1 })] }),
          ligaZi({
            leagueId: 902,
            nume: 'Liga Beta',
            meciuri: [meciZi({ fixtureId: 2, gazde: { id: 7, nume: 'Steaua Verde', logo: null } })],
          }),
        ],
      }),
    );
    randeaza();

    expect(await screen.findByText('Liga Alfa')).toBeInTheDocument();
    expect(screen.getByText('Liga Beta')).toBeInTheDocument();
    expect(screen.getByText('Manchester United')).toBeInTheDocument();
    expect(screen.getByText('Steaua Verde')).toBeInTheDocument();
    // pagina cere meciurile zilei de azi (data locala)
    expect(mockGetMeciuriZi).toHaveBeenCalledTimes(1);
  });

  test('competitiile sunt ordonate: top 5 inaintea restului, amicalele ultimele', async () => {
    // Fiecare competitie are o gazda unica — ordinea lor in DOM da ordinea competitiilor,
    // fara ambiguitate cu numele de liga care apar si in carusel / rail.
    const gazda = (id: number, nume: string) => meciZi({ fixtureId: id, gazde: { id, nume, logo: null } });
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({
        ligi: [
          ligaZi({ leagueId: 667, nume: 'Amicale cluburi', meciuri: [gazda(1, 'Echipa Amicala')] }),
          ligaZi({ leagueId: 283, nume: 'Liga I', meciuri: [gazda(2, 'Echipa Interna')] }),
          ligaZi({ leagueId: 39, nume: 'Premier League', meciuri: [gazda(3, 'Echipa Engleza')] }),
        ],
      }),
    );
    const { container } = randeaza();
    await screen.findByText('Echipa Engleza');

    const text = container.textContent ?? '';
    expect(text.indexOf('Echipa Engleza')).toBeLessThan(text.indexOf('Echipa Interna'));
    expect(text.indexOf('Echipa Interna')).toBeLessThan(text.indexOf('Echipa Amicala'));
  });

  test('filtrul Live pastreaza doar meciurile in desfasurare si scoate ligile golite', async () => {
    const user = userEvent.setup();
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({
        ligi: [
          ligaZi({
            leagueId: 901,
            nume: 'Liga Alfa',
            meciuri: [
              meciZi({ fixtureId: 1, inDesfasurare: true, status: '1H', minut: 20 }),
              meciZi({ fixtureId: 2, gazde: { id: 8, nume: 'Echipa Programata', logo: null } }),
            ],
          }),
          ligaZi({ leagueId: 902, nume: 'Liga Beta', meciuri: [meciZi({ fixtureId: 3 })] }),
        ],
      }),
    );
    randeaza();
    await screen.findByText('Liga Alfa');

    await user.click(screen.getByRole('button', { name: 'Live' }));

    expect(screen.getByText('Liga Alfa')).toBeInTheDocument();
    expect(screen.queryByText('Liga Beta')).toBeNull();
    expect(screen.queryByText('Echipa Programata')).toBeNull();
  });

  test('filtrul Favorite foloseste echipele salvate in localStorage', async () => {
    const user = userEvent.setup();
    localStorage.setItem(
      'golstat-echipe-favorite',
      JSON.stringify({ 7: { id: 7, nume: 'Steaua Verde', logo: null } }),
    );
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({
        ligi: [
          ligaZi({ leagueId: 901, nume: 'Liga Alfa', meciuri: [meciZi({ fixtureId: 1 })] }),
          ligaZi({
            leagueId: 902,
            nume: 'Liga Beta',
            meciuri: [meciZi({ fixtureId: 2, gazde: { id: 7, nume: 'Steaua Verde', logo: null } })],
          }),
        ],
      }),
    );
    randeaza();
    await screen.findByText('Liga Alfa');

    await user.click(screen.getByRole('button', { name: 'Favorite' }));

    expect(screen.queryByText('Liga Alfa')).toBeNull();
    expect(screen.getByText('Liga Beta')).toBeInTheDocument();
  });

  test('filtrul "Începe curând" pastreaza doar meciurile din urmatoarele 2 ore', async () => {
    const user = userEvent.setup();
    const inOra = new Date(Date.now() + 60 * 60 * 1000).toISOString();
    const inCinciOre = new Date(Date.now() + 5 * 60 * 60 * 1000).toISOString();
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({
        ligi: [
          ligaZi({
            leagueId: 901,
            nume: 'Liga Alfa',
            meciuri: [
              meciZi({ fixtureId: 1, kickoff: inOra, gazde: { id: 5, nume: 'Echipa Curand', logo: null } }),
              meciZi({ fixtureId: 2, kickoff: inCinciOre, gazde: { id: 6, nume: 'Echipa Tarziu', logo: null } }),
              meciZi({
                fixtureId: 3,
                inDesfasurare: true,
                status: '1H',
                gazde: { id: 7, nume: 'Echipa In Joc', logo: null },
              }),
            ],
          }),
        ],
      }),
    );
    randeaza();
    await screen.findByText('Echipa Curand');

    await user.click(screen.getByRole('button', { name: 'Începe curând' }));

    expect(screen.getByText('Echipa Curand')).toBeInTheDocument();
    expect(screen.queryByText('Echipa Tarziu')).toBeNull();
    expect(screen.queryByText('Echipa In Joc')).toBeNull();
  });

  test('butonul Filtre reseteaza comutatoarele', async () => {
    const user = userEvent.setup();
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({ ligi: [ligaZi({ leagueId: 901, nume: 'Liga Alfa', meciuri: [meciZi()] })] }),
    );
    randeaza();
    await screen.findByText('Liga Alfa');

    await user.click(screen.getByRole('button', { name: 'Live' }));
    expect(screen.queryByText('Liga Alfa')).toBeNull();

    await user.click(screen.getByRole('button', { name: /Filtre/ }));
    expect(screen.getByText('Liga Alfa')).toBeInTheDocument();
  });

  test('zi fara meciuri: EmptyState', async () => {
    mockGetMeciuriZi.mockResolvedValue(programZiGrupat({ ligi: [] }));
    randeaza();
    expect(await screen.findByText('Niciun meci de afișat')).toBeInTheDocument();
  });

  test('eroare API: ErrorState cu retry care recheama API-ul', async () => {
    const user = userEvent.setup();
    mockGetMeciuriZi
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'Baza de date nu răspunde.'))
      .mockResolvedValueOnce(programZiGrupat({ ligi: [ligaZi({ leagueId: 901, nume: 'Liga Alfa' })] }));
    randeaza();

    expect(await screen.findByText('Serviciu indisponibil')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));

    expect(await screen.findByText('Liga Alfa')).toBeInTheDocument();
    expect(mockGetMeciuriZi).toHaveBeenCalledTimes(2);
  });

  test('nu apare NaN/undefined in pagina cu predictii si scoruri lipsa', async () => {
    mockGetMeciuriZi.mockResolvedValue(
      programZiGrupat({
        ligi: [
          ligaZi({
            leagueId: 901,
            nume: 'Liga Alfa',
            meciuri: [meciZi({ predictie: null, golGazde: null, golOaspeti: null, minut: null, runda: null })],
          }),
        ],
      }),
    );
    const { container } = randeaza();
    await screen.findByText('Liga Alfa');
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
  });
});
