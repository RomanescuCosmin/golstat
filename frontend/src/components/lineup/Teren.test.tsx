import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { jucatorLineup } from '../../test/factories';
import { Teren } from './Teren';

/** Containerul absolut pozitionat al unui jucator (dupa numele afisat). */
function pozitia(nume: string): CSSStyleDeclaration {
  const el = screen.getByText(new RegExp(nume)).closest('[style]') as HTMLElement;
  return el.style;
}

/** Un 4-3-3 minimal pe grid: portar + cate un rand de 2/2/1 (destui pentru regula de 60%). */
function gazdeCuGrid() {
  return [
    jucatorLineup({ id: 1, nume: 'Portar', numar: 1, grid: '1:1' }),
    jucatorLineup({ id: 2, nume: 'FundasA', numar: 2, grid: '2:1' }),
    jucatorLineup({ id: 3, nume: 'FundasB', numar: 3, grid: '2:2' }),
    jucatorLineup({ id: 4, nume: 'MijlocasA', numar: 6, grid: '3:1' }),
    jucatorLineup({ id: 5, nume: 'MijlocasB', numar: 8, grid: '3:2' }),
    jucatorLineup({ id: 6, nume: 'Atacant', numar: 9, grid: '4:1' }),
  ];
}

describe('Teren (pozitionare titulari)', () => {
  test('gazdele pe jumatatea stanga: portarul langa poarta (8%), atacantul spre centru (46%)', () => {
    render(<Teren gazde={gazdeCuGrid()} oaspeti={[]} />);
    expect(pozitia('Portar').left).toBe('8%');
    expect(pozitia('Atacant').left).toBe('46%');
  });

  test('oaspetii sunt oglinditi pe jumatatea dreapta', () => {
    render(
      <Teren
        gazde={[]}
        oaspeti={gazdeCuGrid().map((j) => ({ ...j, id: (j.id ?? 0) + 100, nume: `O${j.nume}` }))}
      />,
    );
    expect(pozitia('OPortar').left).toBe('92%');
    expect(pozitia('OAtacant').left).toBe('54%');
  });

  test('randul cu un singur jucator sta centrat pe verticala, randurile late se impart egal', () => {
    render(<Teren gazde={gazdeCuGrid()} oaspeti={[]} />);
    expect(pozitia('Portar').top).toBe('50%');
    // randul 2 are 2 coloane: (col - 0.5) / 2 * 100 → 25% si 75%
    expect(pozitia('FundasA').top).toBe('25%');
    expect(pozitia('FundasB').top).toBe('75%');
  });

  test('sub 60% grid-uri valide: cade pe distributia uniforma dupa ordine', () => {
    const faraGrid = [
      jucatorLineup({ id: 1, nume: 'Portar', grid: null }),
      jucatorLineup({ id: 2, nume: 'JucatorA', grid: null }),
      jucatorLineup({ id: 3, nume: 'JucatorB', grid: 'invalid' }),
      jucatorLineup({ id: 4, nume: 'JucatorC', grid: '2:1' }),
    ];
    render(<Teren gazde={faraGrid} oaspeti={[]} />);
    // primul jucator e tratat drept portar (rand 1) chiar si fara grid
    expect(pozitia('Portar').left).toBe('8%');
    // urmatorii trei impart randul 2 in 3 coloane
    expect(pozitia('JucatorA').top).toBe(`${(0.5 / 3) * 100}%`);
    expect(pozitia('JucatorB').top).toBe('50%');
  });

  test('jucator fara nume/numar: fallback-uri, fara "null" sau "undefined" afisat', () => {
    const { container } = render(
      <Teren gazde={[jucatorLineup({ id: 1, nume: null, numar: null, grid: '1:1' })]} oaspeti={[]} />,
    );
    expect(container.textContent).not.toMatch(/null|undefined/);
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
