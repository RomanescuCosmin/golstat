import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import type { Predictie1X2 } from '../../api/types';
import { BaraProbabilitate } from './BaraProbabilitate';

function predictie(gazde: number, egal: number, oaspeti: number): Predictie1X2 {
  return {
    gazde: { procent: gazde, cota: 100 / gazde },
    egal: { procent: egal, cota: 100 / egal },
    oaspeti: { procent: oaspeti, cota: 100 / oaspeti },
  };
}

/** Procentele afisate sub bara, in ordinea 1 / X / 2. */
function procenteAfisate(container: HTMLElement): number[] {
  const spans = container.querySelectorAll('.tabular-nums span');
  return Array.from(spans).map((s) => Number.parseInt(s.textContent ?? '', 10));
}

describe('BaraProbabilitate (1X2)', () => {
  test('afiseaza procentele rotunjite pentru fiecare parte', () => {
    render(<BaraProbabilitate predictie={predictie(45.2, 27.9, 26.9)} />);
    expect(screen.getByText('45%')).toBeInTheDocument();
    expect(screen.getByText('28%')).toBeInTheDocument();
    expect(screen.getByText('27%')).toBeInTheDocument();
  });

  test('latimea segmentelor corespunde procentelor afisate', () => {
    const { container } = render(<BaraProbabilitate predictie={predictie(45.2, 27.9, 26.9)} />);
    const [seg1, segX, seg2] = Array.from(
      container.querySelectorAll<HTMLElement>('.h-2 > div'),
    );
    expect(seg1.style.width).toBe('45%');
    expect(segX.style.width).toBe('28%');
    expect(seg2.style.width).toBe('27%');
  });

  test('procentele afisate insumeaza 100% (fara gol vizual in bara)', () => {
    // 33.4 + 33.3 + 33.3 = 100, dar rotunjite independent dau 33+33+33 = 99
    const { container } = render(<BaraProbabilitate predictie={predictie(33.4, 33.3, 33.3)} />);
    const suma = procenteAfisate(container).reduce((a, b) => a + b, 0);
    expect(suma).toBe(100);
  });

  test('procentele afisate nu depasesc 100% insumate', () => {
    // 33.5 + 33.5 + 33.0 = 100, dar rotunjite independent dau 34+34+33 = 101
    const { container } = render(<BaraProbabilitate predictie={predictie(33.5, 33.5, 33.0)} />);
    const suma = procenteAfisate(container).reduce((a, b) => a + b, 0);
    expect(suma).toBeLessThanOrEqual(100);
  });

  test('procente nevalide (NaN): nu afiseaza niciodata "NaN%"', () => {
    const { container } = render(<BaraProbabilitate predictie={predictie(NaN, NaN, NaN)} />);
    expect(container.textContent).not.toContain('NaN');
    for (const seg of container.querySelectorAll<HTMLElement>('.h-2 > div')) {
      expect(seg.style.width).toMatch(/^\d+%$/);
    }
  });

  test('suma bruta peste 100 (date corupte): normalizata la exact 100, pastrand ordinea', () => {
    // 50 + 40 + 30 = 120 — fara normalizare bara ar depasi 100%
    const { container } = render(<BaraProbabilitate predictie={predictie(50, 40, 30)} />);
    const [g, e, o] = procenteAfisate(container);
    expect(g + e + o).toBe(100);
    expect(g).toBeGreaterThan(e);
    expect(e).toBeGreaterThan(o);
  });

  test('toate procentele 0: bara goala cu "0%", fara impartire la zero', () => {
    const { container } = render(<BaraProbabilitate predictie={predictie(0, 0, 0)} />);
    expect(procenteAfisate(container)).toEqual([0, 0, 0]);
    expect(container.textContent).not.toContain('NaN');
  });
});
