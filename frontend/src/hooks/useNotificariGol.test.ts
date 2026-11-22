import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import type { MeciLive } from '../api/types';
import { comutaMeciFavorit } from '../lib/meciuriFavorite';
import { useNotificariGol } from './useNotificariGol';

const getLive = vi.fn<() => Promise<MeciLive[]>>();
vi.mock('../api/client', () => ({ getLive: () => getLive() }));
const sunetGol = vi.fn();
vi.mock('../lib/sunet', () => ({ sunetGol: () => sunetGol(), pregatesteSunet: vi.fn() }));

function live(fixtureId: number, golGazde: number, golOaspeti: number): MeciLive {
  return {
    fixtureId,
    leagueId: 39,
    ligaNume: 'Premier League',
    ligaLogo: null,
    gazde: { id: 10 + fixtureId, nume: 'Gazde', logo: null },
    oaspeti: { id: 20 + fixtureId, nume: 'Oaspeti', logo: null },
    golGazde,
    golOaspeti,
    status: '1H',
    minut: 30,
  };
}

async function tick() {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(15000);
  });
}

describe('useNotificariGol', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.useFakeTimers();
    getLive.mockReset();
    sunetGol.mockReset();
  });
  afterEach(() => vi.useRealTimers());

  test('gol intr-un meci favorit → notificare + sunet; gol intr-un meci nefavorit → nimic', async () => {
    comutaMeciFavorit({
      fixtureId: 1,
      kickoff: new Date().toISOString(),
      gazde: { id: 11, nume: 'Gazde', logo: null },
      oaspeti: { id: 21, nume: 'Oaspeti', logo: null },
      leagueId: 39,
      ligaNume: 'Premier League',
    });
    getLive.mockResolvedValue([live(1, 0, 0), live(2, 0, 0)]);
    const { result } = renderHook(() => useNotificariGol());
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });

    getLive.mockResolvedValue([live(1, 1, 0), live(2, 0, 1)]);
    await tick();

    expect(sunetGol).toHaveBeenCalledTimes(1);
    expect(result.current.notificari.map((n) => n.fixtureId)).toEqual([1]);
    expect(result.current.notificari[0]!.marcatorAcasa).toBe(true);
  });

  test('fara favorite → niciun sunet', async () => {
    getLive.mockResolvedValue([live(1, 0, 0)]);
    renderHook(() => useNotificariGol());
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    getLive.mockResolvedValue([live(1, 2, 0)]);
    await tick();

    expect(sunetGol).not.toHaveBeenCalled();
  });
});
