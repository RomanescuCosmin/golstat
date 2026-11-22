import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test } from 'vitest';
import type { StatisticiEchipaMeci } from '../../api/types';
import { echipa, statisticiEchipaMeci, statisticiMeci } from '../../test/factories';
import { StatisticiLive } from './StatisticiLive';

function repriza(over: Partial<StatisticiEchipaMeci>): StatisticiEchipaMeci {
  return statisticiEchipaMeci({
    posesie: null, pase: null, paseReusite: null, preciziePase: null, xg: null, ...over,
  });
}

const gazde = echipa();
const oaspeti = echipa({ id: 40, nume: 'Liverpool' });

describe('StatisticiLive', () => {
  test('fara reprize: fara taburi, doar statisticile meciului', () => {
    render(<StatisticiLive gazde={gazde} oaspeti={oaspeti} statistici={statisticiMeci()} />);

    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    expect(screen.getByText('Posesie')).toBeInTheDocument();
  });

  test('cu reprize: taburile Meci / Repriza 1 / Repriza 2 schimba statisticile afisate', async () => {
    render(
      <StatisticiLive
        gazde={gazde}
        oaspeti={oaspeti}
        statistici={statisticiMeci()}
        reprize={[
          { repriza: 1, gazde: repriza({ cornere: 4 }), oaspeti: repriza({ cornere: 1 }) },
          { repriza: 2, gazde: repriza({ cornere: 2 }), oaspeti: null },
        ]}
      />,
    );

    expect(screen.getAllByRole('tab').map((t) => t.textContent)).toEqual(['Meci', 'Repriza 1', 'Repriza 2']);
    expect(screen.getByRole('tab', { name: 'Meci' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Posesie')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: 'Repriza 1' }));

    expect(screen.getByRole('tab', { name: 'Repriza 1' })).toHaveAttribute('aria-selected', 'true');
    // posesia / pasele / xG nu exista pe repriza → randurile dispar
    expect(screen.queryByText('Posesie')).not.toBeInTheDocument();
    expect(screen.queryByText('xG')).not.toBeInTheDocument();
    const cornere = screen.getByText('Cornere').parentElement!.parentElement!;
    expect(cornere).toHaveTextContent('4');
    expect(cornere).toHaveTextContent('1');

    await userEvent.click(screen.getByRole('tab', { name: 'Repriza 2' }));
    expect(screen.getByText('Cornere').parentElement!.parentElement!).toHaveTextContent('2');
  });
});
