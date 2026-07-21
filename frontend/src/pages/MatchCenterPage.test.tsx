import { act, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getLive, getMatchCenter, getPrevizualizare } from '../api/client';
import { emiteLive, resetLiveMock } from '../api/__mocks__/live';
import {
  evenimentMeci,
  fixtureLive,
  meciCentral,
  previzualizareMeci,
  statisticiAvansate,
} from '../test/factories';
import { randeazaCuRuta } from '../test/rutare';
import { MatchCenterPage } from './MatchCenterPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getMatchCenter: vi.fn(),
  getLive: vi.fn(),
  getPrevizualizare: vi.fn(),
}));
vi.mock('../api/live');

const mockGetMatchCenter = vi.mocked(getMatchCenter);
const mockGetLive = vi.mocked(getLive);
const mockGetPrevizualizare = vi.mocked(getPrevizualizare);

function randeaza(fixtureId = '2001') {
  return randeazaCuRuta(<MatchCenterPage />, {
    cale: '/meci/:fixtureId/centru',
    ruta: `/meci/${fixtureId}/centru`,
  });
}

beforeEach(() => {
  mockGetMatchCenter.mockReset();
  mockGetLive.mockReset();
  mockGetLive.mockResolvedValue([]);
  mockGetPrevizualizare.mockReset();
  mockGetPrevizualizare.mockResolvedValue(previzualizareMeci());
  resetLiveMock();
});

describe('MatchCenterPage', () => {
  test('happy path: scor, statistici, formatii si cronologie', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral());
    randeaza();

    expect(await screen.findByText('1 – 0')).toBeInTheDocument();
    expect(screen.getAllByText('Manchester United').length).toBeGreaterThan(0);
    expect(screen.getByText('Cronologie')).toBeInTheDocument();
    expect(screen.getByText(/Stadion: Old Trafford/)).toBeInTheDocument();
    expect(screen.getByText(/Arbitru: M\. Oliver/)).toBeInTheDocument();
  });

  test('push WebSocket in-play actualizeaza scorul si minutul din antet', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral({ golGazde: 1, golOaspeti: 0, minut: 30 }));
    randeaza();
    await screen.findByText('1 – 0');

    act(() => emiteLive(2001, fixtureLive({ id: 2001, statusShort: '2H', goalsHome: 2, goalsAway: 1, statusElapsed: 67 })));

    expect(screen.getByText('2 – 1')).toBeInTheDocument();
    expect(screen.getByText("67'")).toBeInTheDocument();
  });

  test('meci fara statistici / fara evenimente: EmptyState pe fiecare sectiune', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral({ statistici: null, formatii: null, evenimente: [] }));
    randeaza();

    expect(await screen.findByText('Statistici indisponibile')).toBeInTheDocument();
    expect(screen.getByText('Fără evenimente')).toBeInTheDocument();
    expect(screen.queryByText('Cronologie')).toBeNull();
  });

  test('meci terminat: badge Final, fara minut', async () => {
    mockGetMatchCenter.mockResolvedValue(
      meciCentral({ inDesfasurare: false, terminat: true, status: 'FT', golGazde: 2, golOaspeti: 2, evenimente: [evenimentMeci()] }),
    );
    randeaza();

    expect(await screen.findByText('2 – 2')).toBeInTheDocument();
    expect(screen.getByText('Final')).toBeInTheDocument();
  });

  test('meci terminat: sectiunea de verdict pe piete, cu badge verde/rosu', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral({ inDesfasurare: false, terminat: true, status: 'FT' }));
    // linia de goluri e 2.5 cu probabilitate 0.55 (modelul favorizeaza „peste"); 3 goluri reale → ✓
    mockGetPrevizualizare.mockResolvedValue(
      previzualizareMeci({
        statistici: statisticiAvansate({
          rezultat: {
            totalGoluri: 3,
            ambeleMarcheaza: true,
            egalFinal: false,
            egalPauza: false,
            golRepriza1: true,
            golRepriza2: true,
            totalCornere: 10,
            totalFaulturi: 24,
            totalCartonase: 5,
            totalSuturi: 25,
            totalSuturiPePoarta: 9,
          },
        }),
      }),
    );
    randeaza();

    expect(await screen.findByText('Verdict pe piețe')).toBeInTheDocument();
    expect(mockGetPrevizualizare).toHaveBeenCalledWith(2001);
    // textul badge-ului e impartit in noduri („✓" + eticheta), deci potrivim pe textContent
    const badges = await screen.findAllByText((_, el) => /^[✓✗]\s/.test(el?.textContent?.trim() ?? ''));
    expect(badges.length).toBeGreaterThan(0);
  });

  test('meci in desfasurare: fara verdict (nu are ce compara)', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral({ inDesfasurare: true, terminat: false, status: '2H' }));
    randeaza();

    await screen.findByText('1 – 0');
    expect(screen.queryByText('Verdict pe piețe')).toBeNull();
    expect(mockGetPrevizualizare).not.toHaveBeenCalled();
  });

  test('verdict indisponibil: mesaj propriu, restul paginii ramane', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral({ inDesfasurare: false, terminat: true, status: 'FT' }));
    mockGetPrevizualizare.mockRejectedValue(new ApiError(404, 'Not Found', 'Fara istoric.'));
    randeaza();

    expect(await screen.findByText('Verdict indisponibil')).toBeInTheDocument();
    expect(screen.getByText('Cronologie')).toBeInTheDocument();
  });

  test('404: mesaj dedicat', async () => {
    mockGetMatchCenter.mockRejectedValue(new ApiError(404, 'Not Found', 'Meciul nu există.'));
    randeaza();
    expect(await screen.findByText('Meciul nu a fost găsit')).toBeInTheDocument();
  });

  test('id invalid: eroare fara apel API', async () => {
    randeaza('abc');
    expect(await screen.findByText('Meci invalid')).toBeInTheDocument();
    expect(mockGetMatchCenter).not.toHaveBeenCalled();
  });
});
