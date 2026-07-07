import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import type { FazaEliminatorie } from '../../api/types';
import { echipa, meciCompetitie } from '../../test/factories';
import { SchemaEliminatorie } from './SchemaEliminatorie';

function randeaza(faze: FazaEliminatorie[]) {
  return render(
    <MemoryRouter>
      <SchemaEliminatorie faze={faze} />
    </MemoryRouter>,
  );
}

describe('SchemaEliminatorie', () => {
  test('traduce rundele API-Football in romana; necunoscutele raman ca atare', () => {
    randeaza([
      { runda: 'Round of 16', meciuri: [meciCompetitie({ fixtureId: 1 })] },
      { runda: 'Quarter-finals', meciuri: [meciCompetitie({ fixtureId: 2 })] },
      { runda: 'Semi-finals', meciuri: [meciCompetitie({ fixtureId: 3 })] },
      { runda: 'Final', meciuri: [meciCompetitie({ fixtureId: 4 })] },
      { runda: 'Runda Speciala', meciuri: [meciCompetitie({ fixtureId: 5 })] },
    ]);
    for (const titlu of ['Optimi', 'Sferturi', 'Semifinale', 'Finală', 'Runda Speciala']) {
      expect(screen.getByRole('heading', { name: titlu })).toBeInTheDocument();
    }
  });

  test('meci terminat: invingatorul e ingrosat, scorurile apar', () => {
    randeaza([
      {
        runda: 'Final',
        meciuri: [meciCompetitie({ golGazde: 2, golOaspeti: 1, status: 'FT', terminat: true })],
      },
    ]);
    expect(screen.getByText('Manchester United')).toHaveClass('font-bold');
    expect(screen.getByText('Liverpool')).toHaveClass('font-medium');
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  test('meci viitor: fara scor, data programata afisata', () => {
    randeaza([{ runda: 'Final', meciuri: [meciCompetitie({ kickoff: '2026-07-19T22:00:00+03:00' })] }]);
    expect(screen.getByText('19.07.2026')).toBeInTheDocument();
    expect(screen.queryByText('LIVE')).toBeNull();
  });

  test('meci in desfasurare: eticheta LIVE', () => {
    randeaza([
      {
        runda: 'Final',
        meciuri: [meciCompetitie({ golGazde: 1, golOaspeti: 1, status: '2H', inDesfasurare: true })],
      },
    ]);
    expect(screen.getByText('LIVE')).toBeInTheDocument();
  });

  test('echipa necunoscuta: "TBD"; meci fara kickoff: "De programat"', () => {
    randeaza([
      {
        runda: 'Final',
        meciuri: [
          meciCompetitie({
            gazde: echipa({ nume: null }),
            oaspeti: echipa({ id: 40, nume: null }),
            kickoff: '',
          }),
        ],
      },
    ]);
    expect(screen.getAllByText('TBD')).toHaveLength(2);
    expect(screen.getByText('De programat')).toBeInTheDocument();
  });

  test('fara faze: EmptyState', () => {
    randeaza([]);
    expect(screen.getByText('Fazele eliminatorii nu sunt încă disponibile')).toBeInTheDocument();
  });
});
