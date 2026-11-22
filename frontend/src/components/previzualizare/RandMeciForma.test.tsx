import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { echipa, formaMeci } from '../../test/factories';
import { RandMeciForma } from './RandMeciForma';

const urmarita = echipa({ id: 1, nume: 'Rapid', logo: 'rapid.png' });
const adversar = echipa({ id: 2, nume: 'Dinamo', logo: 'dinamo.png' });

function randeaza(over: Parameters<typeof formaMeci>[0], cuData = true) {
  const { container } = render(
    <ul>
      <RandMeciForma meci={formaMeci({ adversar, ...over })} echipa={urmarita} cuData={cuData} />
    </ul>,
  );
  const li = container.querySelector('li')!;
  const nume = [...li.querySelectorAll('img')].map((i) => i.getAttribute('alt'));
  return { li, nume };
}

describe('RandMeciForma', () => {
  test('acasa: echipa urmarita e gazda, scorul = marcate-primite', () => {
    const { li, nume } = randeaza({ acasa: true, golMarcate: 3, golPrimite: 1, rezultat: 'V' });
    expect(nume).toEqual(['Rapid', 'Dinamo']);
    expect(li).toHaveTextContent('3-1');
    expect(screen.getByText('Rapid')).toHaveClass('font-bold');
    expect(screen.getByText('Dinamo')).not.toHaveClass('font-bold');
    expect(screen.getByTitle('acasă')).toHaveTextContent('V');
  });

  test('deplasare: adversarul e gazda si scorul e in ordinea gazde-oaspeti', () => {
    // Rapid castiga 2-0 in deplasare → pe tabel: Dinamo 0-2 Rapid
    const { li, nume } = randeaza({ acasa: false, golMarcate: 2, golPrimite: 0, rezultat: 'V' });
    expect(nume).toEqual(['Dinamo', 'Rapid']);
    expect(li).toHaveTextContent('0-2');
    expect(li).not.toHaveTextContent('2-0');
    expect(screen.getByText('Rapid')).toHaveClass('font-bold');
    expect(screen.getByTitle('deplasare')).toHaveTextContent('V');
  });

  test('chip-ul de rezultat are culoarea potrivita', () => {
    randeaza({ rezultat: 'I', golMarcate: 0, golPrimite: 1 });
    expect(screen.getByText('I')).toHaveClass('bg-accent');
  });

  test('adversar necunoscut: „Necunoscut", fara crash', () => {
    randeaza({ adversar: null });
    expect(screen.getByText('Necunoscut')).toBeInTheDocument();
  });

  test('data apare doar cand e ceruta', () => {
    expect(randeaza({ data: '2026-11-08' }).li.querySelectorAll('span')[0]).toHaveClass('tabular-nums');
  });

  test('fara data: primul element e direct grila meciului', () => {
    const { li } = randeaza({}, false);
    expect(li.firstElementChild).toHaveClass('grid');
  });
});
