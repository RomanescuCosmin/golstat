import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test } from 'vitest';
import { IntervalGoluri, sansaInterval } from './IntervalGoluri';

const gazde = { id: 1, nume: 'Parma', logo: null };
const oaspeti = { id: 2, nume: 'AS Roma', logo: null };
// P(exact k goluri), k = 0..6, ultima = 7+
const distributie = [0.08, 0.17, 0.23, 0.22, 0.16, 0.09, 0.03, 0.02];
const interval = { distributie, totaluriGazde: [0, 2, 3, 5, 1, 2, 4], totaluriOaspeti: [1, 1, 3, 0, 2] };

async function alege(titlu: string, valoare: string) {
  await userEvent.click(screen.getByRole('button', { name: titlu }));
  await userEvent.click(within(screen.getByRole('listbox', { name: titlu })).getByRole('option', { name: valoare }));
}

describe('IntervalGoluri', () => {
  test('sansaInterval = suma distributiei pe a..b', () => {
    expect(sansaInterval(distributie, 1, 3)).toBeCloseTo(0.62, 10);
    expect(sansaInterval(distributie, 2, 2)).toBeCloseTo(0.23, 10);
  });

  test('implicit 1–3: procentul modelului si in cate meciuri din fereastra s-a intamplat', () => {
    render(<IntervalGoluri interval={interval} gazde={gazde} oaspeti={oaspeti} />);
    expect(screen.getByText('Între 1 și 3 goluri')).toBeInTheDocument();
    expect(screen.getByText(/62/)).toBeInTheDocument();
    // gazde: 2, 3, 1, 2 → 4/7; oaspeti: 1, 1, 3, 2 → 4/5
    expect(screen.getByText('Parma 4/7 · AS Roma 4/5')).toBeInTheDocument();
  });

  test('schimbi limitele: exact 2 goluri si 3–6', async () => {
    render(<IntervalGoluri interval={interval} gazde={gazde} oaspeti={oaspeti} />);
    await alege('De la', '2');
    await alege('Până la', '2');
    expect(screen.getByText('Exact 2 goluri')).toBeInTheDocument();
    expect(screen.getByText('Parma 2/7 · AS Roma 1/5')).toBeInTheDocument();

    await alege('Până la', '6');
    await alege('De la', '3');
    expect(screen.getByText('Între 3 și 6 goluri')).toBeInTheDocument();
  });

  test('„Până la” nu ofera valori sub „De la”', async () => {
    render(<IntervalGoluri interval={interval} gazde={gazde} oaspeti={oaspeti} />);
    await userEvent.click(screen.getByRole('button', { name: 'Până la' }));
    const optiuni = within(screen.getByRole('listbox', { name: 'Până la' })).getAllByRole('option');
    expect(optiuni.map((o) => o.firstElementChild!.textContent)).toEqual(['1', '2', '3', '4', '5', '6']);
  });

  test('meci terminat: verdictul pe totalul real', () => {
    render(<IntervalGoluri interval={interval} gazde={gazde} oaspeti={oaspeti} totalReal={2} />);
    expect(screen.getByText(/✓/)).toBeInTheDocument();
  });

  test('fara date: nu se afiseaza', () => {
    const { container } = render(<IntervalGoluri interval={null} gazde={gazde} oaspeti={oaspeti} />);
    expect(container).toBeEmptyDOMElement();
  });
});
