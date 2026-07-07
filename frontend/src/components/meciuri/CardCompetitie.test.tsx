import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import type { LigaZi } from '../../api/types';
import { ligaZi, meciZi } from '../../test/factories';
import { CardCompetitie } from './CardCompetitie';

function randeaza(liga: LigaZi) {
  return render(
    <MemoryRouter>
      <CardCompetitie liga={liga} />
    </MemoryRouter>,
  );
}

describe('CardCompetitie', () => {
  test('antet: nume, tara si runda scurtata ("Regular Season - 15" → "Etapa 15")', () => {
    randeaza(ligaZi({ meciuri: [meciZi({ runda: 'Regular Season - 15' })] }));
    expect(screen.getByText('Premier League')).toBeInTheDocument();
    expect(screen.getByText('Anglia · Etapa 15')).toBeInTheDocument();
  });

  test('runda nerecunoscuta ramane textul brut', () => {
    randeaza(ligaZi({ meciuri: [meciZi({ runda: 'Grupa A' })] }));
    expect(screen.getByText('Anglia · Grupa A')).toBeInTheDocument();
  });

  test('numaratoarea: "1 meci" la singular, "N meciuri" la plural', () => {
    const { unmount } = randeaza(ligaZi({ meciuri: [meciZi()] }));
    expect(screen.getByText(/1\s+meci$/)).toBeInTheDocument();
    unmount();

    randeaza(ligaZi({ meciuri: [meciZi({ fixtureId: 1 }), meciZi({ fixtureId: 2 })] }));
    expect(screen.getByText(/2\s+meciuri/)).toBeInTheDocument();
  });

  test('colaps: click pe antet ascunde si re-arata meciurile', async () => {
    const user = userEvent.setup();
    randeaza(ligaZi({ meciuri: [meciZi()] }));
    expect(screen.getByText('Manchester United')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Premier League/ }));
    expect(screen.queryByText('Manchester United')).toBeNull();

    await user.click(screen.getByRole('button', { name: /Premier League/ }));
    expect(screen.getByText('Manchester United')).toBeInTheDocument();
  });

  test('peste 6 meciuri: doar 6 vizibile, apoi "Vezi toate" le arata pe toate', async () => {
    const user = userEvent.setup();
    const meciuri = Array.from({ length: 8 }, (_, i) =>
      meciZi({ fixtureId: i + 1, gazde: { id: i + 1, nume: `Gazda ${i + 1}`, logo: null } }),
    );
    randeaza(ligaZi({ meciuri }));

    expect(screen.getAllByText(/^Gazda \d+$/)).toHaveLength(6);
    await user.click(screen.getByRole('button', { name: 'Vezi toate cele 8 meciuri' }));
    expect(screen.getAllByText(/^Gazda \d+$/)).toHaveLength(8);
    await user.click(screen.getByRole('button', { name: 'Arată mai puține' }));
    expect(screen.getAllByText(/^Gazda \d+$/)).toHaveLength(6);
  });

  test('pastila Live apare doar cand exista un meci in desfasurare', () => {
    const { unmount } = randeaza(ligaZi({ meciuri: [meciZi({ inDesfasurare: true, status: '1H', minut: 30 })] }));
    expect(screen.getAllByText('Live').length).toBeGreaterThan(0);
    unmount();

    randeaza(ligaZi({ meciuri: [meciZi()] }));
    expect(screen.queryByText('Live')).toBeNull();
  });
});
