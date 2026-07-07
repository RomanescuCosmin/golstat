import { act, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, getLive, getMatchCenter } from '../api/client';
import { emiteLive, resetLiveMock } from '../api/__mocks__/live';
import { evenimentMeci, fixtureLive, meciCentral } from '../test/factories';
import { randeazaCuRuta } from '../test/rutare';
import { MatchCenterPage } from './MatchCenterPage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getMatchCenter: vi.fn(),
  getLive: vi.fn(),
}));
vi.mock('../api/live');

const mockGetMatchCenter = vi.mocked(getMatchCenter);
const mockGetLive = vi.mocked(getLive);

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
