import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { BandaZile } from './BandaZile';

// Zi fixa (7 iulie 2026, ora locala 12) ca banda sa fie determinista.
beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date(2026, 6, 7, 12, 0, 0));
});

afterEach(() => {
  vi.useRealTimers();
});

function zileDinBanda(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>('button[aria-pressed]'));
}

describe('BandaZile', () => {
  test('banda are azi ± 21 de zile (43 de celule), de la 16 iun la 28 iul', () => {
    const { container } = render(<BandaZile selectata="2026-07-07" onSelect={() => {}} />);
    const zile = zileDinBanda(container);
    expect(zile).toHaveLength(43);
    expect(zile[0]).toHaveTextContent('16 iun');
    expect(zile[42]).toHaveTextContent('28 iul');
  });

  test('ziua de azi e etichetata "Astăzi" si e selectata cand coincide', () => {
    const { container } = render(<BandaZile selectata="2026-07-07" onSelect={() => {}} />);
    const azi = screen.getByText('Astăzi').closest('button');
    expect(azi).toHaveAttribute('aria-pressed', 'true');
    expect(zileDinBanda(container).filter((b) => b.getAttribute('aria-pressed') === 'true')).toHaveLength(1);
  });

  test('alta zi selectata: "Astăzi" ramane pe azi, dar selectia se muta', () => {
    render(<BandaZile selectata="2026-07-09" onSelect={() => {}} />);
    expect(screen.getByText('Astăzi').closest('button')).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByText('9 iul').closest('button')).toHaveAttribute('aria-pressed', 'true');
  });

  test('click pe o zi: onSelect primeste ISO-ul local, fara salt de fus orar', () => {
    const onSelect = vi.fn();
    render(<BandaZile selectata="2026-07-07" onSelect={onSelect} />);
    fireEvent.click(screen.getByText('8 iul').closest('button')!);
    expect(onSelect).toHaveBeenCalledWith('2026-07-08');
  });
});
