import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import type { RandClasament } from '../../api/types';
import { randClasament } from '../../test/factories';
import { ClasamentSnippet } from './ClasamentSnippet';

/** Clasament cu `n` echipe: Echipa 1 … Echipa n, teamId 1 … n. */
function clasament(n: number, over: (rank: number) => Partial<RandClasament> = () => ({})): RandClasament[] {
  return Array.from({ length: n }, (_, i) =>
    randClasament({ rank: i + 1, teamId: i + 1, nume: `Echipa ${i + 1}`, echipaCurenta: false, ...over(i + 1) }),
  );
}

function randeaza(randuri: RandClasament[], teamId: number, compact = true) {
  return render(
    <MemoryRouter>
      <ClasamentSnippet randuri={randuri} teamId={teamId} compact={compact} />
    </MemoryRouter>,
  );
}

function numeAfisate(): string[] {
  return screen.getAllByText(/^Echipa \d+$/).map((el) => el.textContent ?? '');
}

describe('ClasamentSnippet (fereastra compacta)', () => {
  test('echipa la mijlocul clasamentului: fereastra centrata pe ea (±2)', () => {
    randeaza(clasament(10), 5);
    expect(numeAfisate()).toEqual(['Echipa 3', 'Echipa 4', 'Echipa 5', 'Echipa 6', 'Echipa 7']);
  });

  test('echipa in varf: fereastra porneste de la primul loc', () => {
    randeaza(clasament(10), 1);
    expect(numeAfisate()).toEqual(['Echipa 1', 'Echipa 2', 'Echipa 3', 'Echipa 4', 'Echipa 5']);
  });

  test('echipa la coada: fereastra nu depaseste clasamentul', () => {
    randeaza(clasament(10), 10);
    expect(numeAfisate()).toEqual(['Echipa 6', 'Echipa 7', 'Echipa 8', 'Echipa 9', 'Echipa 10']);
  });

  test('echipa negasita: primele 5 randuri', () => {
    randeaza(clasament(10), 99);
    expect(numeAfisate()).toEqual(['Echipa 1', 'Echipa 2', 'Echipa 3', 'Echipa 4', 'Echipa 5']);
  });

  test('clasament mai mic decat fereastra: toate randurile', () => {
    randeaza(clasament(3), 2);
    expect(numeAfisate()).toHaveLength(3);
  });

  test('necompact: tot clasamentul, cu randul echipei curente evidentiat', () => {
    randeaza(clasament(10), 8, false);
    expect(numeAfisate()).toHaveLength(10);
    const randCurent = screen.getByText('Echipa 8').closest('tr');
    expect(randCurent?.className).toContain('bg-primary/10');
    expect(screen.getByText('Echipa 7').closest('tr')?.className).not.toContain('bg-primary/10');
  });

  test('golaveraj pozitiv cu "+", null cu "—"', () => {
    randeaza(
      clasament(3, (rank) => (rank === 1 ? { golaveraj: 7 } : rank === 2 ? { golaveraj: -3 } : { golaveraj: null })),
      1,
    );
    expect(screen.getByText('+7')).toBeInTheDocument();
    expect(screen.getByText('-3')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  test('clasament gol: nu randeaza nimic', () => {
    const { container } = randeaza([], 1);
    expect(container.firstChild).toBeNull();
  });
});
