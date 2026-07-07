import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { BaraComparativa } from './BaraComparativa';

function bare(container: HTMLElement): [HTMLElement, HTMLElement] {
  const [gazde, oaspeti] = Array.from(container.querySelectorAll<HTMLElement>('.h-\\[5px\\]'));
  return [gazde, oaspeti];
}

describe('BaraComparativa', () => {
  test('barele sunt proportionale cu maximul randului', () => {
    const { container } = render(<BaraComparativa eticheta="Cornere" gazde={10} oaspeti={5} />);
    const [g, o] = bare(container);
    expect(g.style.width).toBe('100%');
    expect(o.style.width).toBe('50%');
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  test('valoare null: "—" si bara goala (nu un 0 fals)', () => {
    const { container } = render(<BaraComparativa eticheta="xG" gazde={null} oaspeti={1.7} />);
    const [g, o] = bare(container);
    expect(g.style.width).toBe('0%');
    expect(o.style.width).toBe('100%');
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText('1,7')).toBeInTheDocument();
  });

  test('valorile mici raman vizibile (latime minima 8%)', () => {
    const { container } = render(<BaraComparativa eticheta="Șuturi" gazde={100} oaspeti={1} />);
    const [, o] = bare(container);
    expect(o.style.width).toBe('8%');
  });

  test('ambele zero: bare goale, fara NaN din impartirea la zero', () => {
    const { container } = render(<BaraComparativa eticheta="Cornere" gazde={0} oaspeti={0} />);
    const [g, o] = bare(container);
    expect(g.style.width).toBe('0%');
    expect(o.style.width).toBe('0%');
    expect(container.textContent).not.toContain('NaN');
  });

  test('modul procent: valorile au sufixul %', () => {
    render(<BaraComparativa eticheta="Posesie" gazde={55.4} oaspeti={44.6} procent />);
    expect(screen.getByText('55%')).toBeInTheDocument();
    expect(screen.getByText('45%')).toBeInTheDocument();
  });
});
