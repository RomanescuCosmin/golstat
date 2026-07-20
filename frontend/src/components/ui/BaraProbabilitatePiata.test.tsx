import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BaraProbabilitatePiata } from './BaraProbabilitatePiata';

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
});
