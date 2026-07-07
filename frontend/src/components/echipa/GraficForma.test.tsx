import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import type { MeciForma } from '../../api/types';
import { meciForma } from '../../test/factories';
import { GraficForma } from './GraficForma';

/** Meciuri cu rezultatele date, cele mai recente primele (conventia backend-ului). */
function rezultate(seria: Array<'V' | 'E' | 'I'>): MeciForma[] {
  return seria.map((rezultat, i) =>
    meciForma({ fixtureId: 100 + i, rezultat, runda: `Regular Season - ${seria.length - i}` }),
  );
}

/** Textele <title> ale punctelor unei serii, in ordine cronologica. */
function titluri(container: HTMLElement): string[] {
  return Array.from(container.querySelectorAll('circle title')).map((t) => t.textContent ?? '');
}

describe('GraficForma', () => {
  test('sub 2 meciuri: EmptyState in loc de grafic', () => {
    const { container } = render(<GraficForma rezultate={rezultate(['V'])} />);
    expect(screen.getByText('Date insuficiente')).toBeInTheDocument();
    expect(container.querySelector('svg[role="img"]')).toBeNull();
  });

  test('deseneaza 3 serii (V/E/I) cu cate un punct per meci', () => {
    const { container } = render(<GraficForma rezultate={rezultate(['V', 'E', 'I', 'V'])} />);
    expect(container.querySelectorAll('path[fill="none"]')).toHaveLength(3);
    expect(container.querySelectorAll('circle')).toHaveLength(12);
  });

  test('numarul rulant pe fereastra de 5: sase victorii la rand plafoneaza la 5', () => {
    const { container } = render(<GraficForma rezultate={rezultate(['V', 'V', 'V', 'V', 'V', 'V'])} />);
    const victorii = titluri(container).filter((t) => t.includes('Victorii'));
    // cronologic: 1,2,3,4,5 apoi fereastra plina ramane la 5
    expect(victorii.map((t) => t.split(': ')[1])).toEqual(['1', '2', '3', '4', '5', '5']);
  });

  test('fereastra alunecatoare uita meciurile vechi', () => {
    // cronologic: V V V V V I → la ultima etapa fereastra e [V V V V I]: 4 victorii, 1 infrangere
    const { container } = render(<GraficForma rezultate={rezultate(['I', 'V', 'V', 'V', 'V', 'V'])} />);
    const victorii = titluri(container).filter((t) => t.includes('Victorii'));
    const infrangeri = titluri(container).filter((t) => t.includes('Înfrângeri'));
    const ultimaVictorie = victorii[victorii.length - 1];
    const ultimaInfrangere = infrangeri[infrangeri.length - 1];
    expect(ultimaVictorie).toContain(': 4');
    expect(ultimaInfrangere).toContain(': 1');
  });

  test('etichetele etapelor vin din runda ("Et. N")', () => {
    const { container } = render(<GraficForma rezultate={rezultate(['V', 'E'])} />);
    expect(container.textContent).toContain('Et. 1');
    expect(container.textContent).toContain('Et. 2');
  });
});
