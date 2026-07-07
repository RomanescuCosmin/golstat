import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { getMatchCenter } from '../api/client';
import { resetLiveMock } from '../api/__mocks__/live';
import { meciCentral } from '../test/factories';
import { useMatchCenter } from './useMatchCenter';

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  getMatchCenter: vi.fn(),
}));
vi.mock('../api/live');

const mockGetMatchCenter = vi.mocked(getMatchCenter);

beforeEach(() => {
  mockGetMatchCenter.mockReset();
  resetLiveMock();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('useMatchCenter', () => {
  test('fetch initial: loading → date', async () => {
    mockGetMatchCenter.mockResolvedValue(meciCentral({ fixtureId: 2001 }));
    const { result } = renderHook(() => useMatchCenter('2001'));

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.date?.fixtureId).toBe(2001);
    expect(result.current.eroare).toBeNull();
  });

  test.each(['abc', '0', '-3', undefined])('id invalid (%s): eroare fara niciun fetch', async (id) => {
    const { result } = renderHook(() => useMatchCenter(id));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.eroare?.title).toBe('Meci invalid');
    expect(mockGetMatchCenter).not.toHaveBeenCalled();
  });

  test('meci in desfasurare: re-interogheaza la fiecare 15s', async () => {
    vi.useFakeTimers();
    mockGetMatchCenter.mockResolvedValue(meciCentral({ inDesfasurare: true }));
    const { result } = renderHook(() => useMatchCenter('2001'));

    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    expect(result.current.date).not.toBeNull();
    expect(mockGetMatchCenter).toHaveBeenCalledTimes(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_000);
    });
    expect(mockGetMatchCenter).toHaveBeenCalledTimes(2);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_000);
    });
    expect(mockGetMatchCenter).toHaveBeenCalledTimes(3);
  });

  test('meci terminat: fara polling', async () => {
    vi.useFakeTimers();
    mockGetMatchCenter.mockResolvedValue(meciCentral({ inDesfasurare: false, terminat: true, status: 'FT' }));
    renderHook(() => useMatchCenter('2001'));

    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000);
    });
    expect(mockGetMatchCenter).toHaveBeenCalledTimes(1);
  });

  test('eroare tranzitorie la poll: datele curente raman', async () => {
    vi.useFakeTimers();
    mockGetMatchCenter
      .mockResolvedValueOnce(meciCentral({ inDesfasurare: true, golGazde: 1 }))
      .mockRejectedValueOnce(new Error('retea'));
    const { result } = renderHook(() => useMatchCenter('2001'));

    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_000);
    });
    expect(result.current.date?.golGazde).toBe(1);
    expect(result.current.eroare).toBeNull();
  });

  test('reincarca(): refetch dupa o eroare', async () => {
    const { ApiError } = await import('../api/client');
    mockGetMatchCenter
      .mockRejectedValueOnce(new ApiError(503, 'Serviciu indisponibil', 'DB down'))
      .mockResolvedValueOnce(meciCentral());
    const { result } = renderHook(() => useMatchCenter('2001'));

    await waitFor(() => expect(result.current.eroare?.title).toBe('Serviciu indisponibil'));

    act(() => result.current.reincarca());
    await waitFor(() => expect(result.current.date).not.toBeNull());
    expect(result.current.eroare).toBeNull();
    expect(mockGetMatchCenter).toHaveBeenCalledTimes(2);
  });
});
