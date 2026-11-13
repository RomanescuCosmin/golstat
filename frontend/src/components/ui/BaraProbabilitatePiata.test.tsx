import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BaraProbabilitatePiata, culoareProbabilitate } from './BaraProbabilitatePiata';

describe('BaraProbabilitatePiata', () => {
  it('umple bara proportional cand exista istoric', () => {
    render(<BaraProbabilitatePiata rata={0.62} />);
    expect(screen.getByTestId('umplere-bara')).toHaveStyle({ width: '62%' });
  });

  it('NU randeaza umplerea cand nu exista istoric', () => {
    render(<BaraProbabilitatePiata rata={0.62} areIstoric={false} />);
    expect(screen.queryByTestId('umplere-bara')).not.toBeInTheDocument();
  });

  it('trateaza valorile ne-finite ca 0, fara NaN in stil', () => {
    const { container } = render(<BaraProbabilitatePiata rata={NaN} />);
    expect(screen.getByTestId('umplere-bara')).toHaveStyle({ width: '0%' });
    expect(container.innerHTML).not.toMatch(/NaN/);
  });

  it('plafoneaza peste 100% si sub 0%', () => {
    const { rerender } = render(<BaraProbabilitatePiata rata={1.8} />);
    expect(screen.getByTestId('umplere-bara')).toHaveStyle({ width: '100%' });
    rerender(<BaraProbabilitatePiata rata={-0.4} />);
    expect(screen.getByTestId('umplere-bara')).toHaveStyle({ width: '0%' });
  });

  it('coloreaza dupa prag: >=70 verde, 50-69 galben, sub 50 rosu', () => {
    expect(culoareProbabilitate(0.7).bara).toBe('bg-green-500');
    expect(culoareProbabilitate(0.95).text).toBe('text-green-500');
    expect(culoareProbabilitate(0.69).bara).toBe('bg-yellow-500');
    expect(culoareProbabilitate(0.5).bara).toBe('bg-yellow-500');
    expect(culoareProbabilitate(0.49).bara).toBe('bg-red-500');
    expect(culoareProbabilitate(NaN).bara).toBe('bg-red-500');
  });

  it('bara de pe pagina de meci foloseste culoarea pragului', () => {
    render(<BaraProbabilitatePiata rata={0.75} />);
    expect(screen.getByTestId('umplere-bara')).toHaveClass('bg-green-500');
  });
});
