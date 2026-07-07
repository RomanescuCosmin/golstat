import { act, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { getLive } from '../api/client';
import { emiteLive, resetLiveMock } from '../api/__mocks__/live';
import { fixtureLive, meciLive } from '../test/factories';
import { LivePage } from './LivePage';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getLive: vi.fn(),
}));
vi.mock('../api/live');

const mockGetLive = vi.mocked(getLive);

function randeaza() {
  return render(
    <MemoryRouter>
      <LivePage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mockGetLive.mockReset();
  resetLiveMock();
});

describe('LivePage', () => {
  test('grupeaza meciurile pe competitie, in ordinea endpoint-ului', async () => {
    mockGetLive.mockResolvedValue([
      meciLive({ fixtureId: 1, minut: 30 }),
      meciLive({ fixtureId: 2, gazde: { id: 50, nume: 'Manchester City', logo: null }, minut: 12 }),
      meciLive({ fixtureId: 3, leagueId: 140, ligaNume: 'La Liga', minut: 55 }),
    ]);
    randeaza();

    expect(await screen.findByText('Premier League')).toBeInTheDocument();
    expect(screen.getByText('La Liga')).toBeInTheDocument();
    // doua sectiuni + numaratoarea din antet
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  test('meci fara liga in DB: sectiunea se numeste "Alte competiții", nu "Liga #0"', async () => {
    mockGetLive.mockResolvedValue([meciLive({ leagueId: null, ligaNume: null })]);
    randeaza();

    expect(await screen.findByText('Alte competiții')).toBeInTheDocument();
    expect(screen.queryByText('Liga #0')).toBeNull();
  });

  test('meci fara leagueId dar cu nume de liga: foloseste numele', async () => {
    mockGetLive.mockResolvedValue([meciLive({ leagueId: null, ligaNume: 'Cupa României' })]);
    randeaza();
    expect(await screen.findByText('Cupa României')).toBeInTheDocument();
  });

  test('push WebSocket in-play suprascrie scorul si minutul din snapshot', async () => {
    mockGetLive.mockResolvedValue([meciLive({ fixtureId: 2001, golGazde: 1, golOaspeti: 0, minut: 30 })]);
    randeaza();
    expect(await screen.findByText('1 – 0')).toBeInTheDocument();

    act(() => emiteLive(2001, fixtureLive({ id: 2001, statusShort: '2H', goalsHome: 2, goalsAway: 0, statusElapsed: 67 })));

    expect(screen.getByText('2 – 0')).toBeInTheDocument();
    expect(screen.getByText("67'")).toBeInTheDocument();
  });

  test('push HT: eticheta devine "Pauză"', async () => {
    mockGetLive.mockResolvedValue([meciLive({ fixtureId: 2001 })]);
    randeaza();
    await screen.findByText('1 – 0');

    act(() => emiteLive(2001, fixtureLive({ id: 2001, statusShort: 'HT', goalsHome: 1, goalsAway: 0, statusElapsed: 45 })));

    expect(screen.getByText('Pauză')).toBeInTheDocument();
  });

  test('push FT (nu mai e in-play): snapshot-ul din DB ramane, nu e suprascris', async () => {
    mockGetLive.mockResolvedValue([meciLive({ fixtureId: 2001, golGazde: 1, golOaspeti: 0, minut: 30 })]);
    randeaza();
    await screen.findByText('1 – 0');

    act(() => emiteLive(2001, fixtureLive({ id: 2001, statusShort: 'FT', goalsHome: 3, goalsAway: 2 })));

    expect(screen.getByText('1 – 0')).toBeInTheDocument();
    expect(screen.queryByText('3 – 2')).toBeNull();
  });

  test('fara meciuri live: EmptyState', async () => {
    mockGetLive.mockResolvedValue([]);
    randeaza();
    expect(await screen.findByText('Niciun meci în desfășurare acum')).toBeInTheDocument();
  });

  test('nu apare NaN/undefined in pagina cu scoruri null', async () => {
    mockGetLive.mockResolvedValue([meciLive({ golGazde: null, golOaspeti: null, minut: null })]);
    const { container } = randeaza();
    await screen.findByText('Premier League');
    expect(container.textContent).not.toMatch(/NaN|undefined|Infinity/);
    // scor lipsa → 0 – 0, minut lipsa → LIVE
    expect(screen.getByText('0 – 0')).toBeInTheDocument();
    expect(screen.getByText('LIVE')).toBeInTheDocument();
  });
});
