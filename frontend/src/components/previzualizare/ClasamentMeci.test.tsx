import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { getClasament } from '../../api/client';
import { clasamentDetaliat, meciFormaClasament, randClasamentDetaliat } from '../../test/factories';
import { ClasamentMeci } from './ClasamentMeci';

vi.mock('../../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/client')>()),
  getClasament: vi.fn(),
}));

const mockGetClasament = vi.mocked(getClasament);

function randeaza(gazdeId = 33, oaspetiId = 40) {
  return render(
    <MemoryRouter>
      <ClasamentMeci leagueId={39} sezon={2026} gazdeId={gazdeId} oaspetiId={oaspetiId} />
    </MemoryRouter>,
  );
}

const randuri = () => screen.getAllByRole('row').slice(1);

beforeEach(() => {
  mockGetClasament.mockReset();
});

describe('ClasamentMeci', () => {
  test('cere clasamentul competitiei si sezonului meciului', async () => {
    mockGetClasament.mockResolvedValue(clasamentDetaliat());
    randeaza();
    await screen.findByText('Burnley');
    expect(mockGetClasament).toHaveBeenCalledWith(39, 2026);
  });

  test('un singur clasament: titlu „Clasament", fara selector de grupa, ordinea oficiala', async () => {
    mockGetClasament.mockResolvedValue(clasamentDetaliat());
    randeaza();
    await screen.findByText('Burnley');

    expect(screen.getByRole('heading', { name: 'Clasament' })).toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Grupa' })).toBeNull();
    expect(randuri().map((r) => within(r).getAllByRole('cell')[1]!.querySelector('span.truncate')?.textContent)).toEqual([
      'Manchester United',
      'Liverpool',
      'Burnley',
    ]);
  });

  test('coloanele: MJ, V/E/Î, goluri, golaveraj cu „+", puncte', async () => {
    mockGetClasament.mockResolvedValue(clasamentDetaliat());
    randeaza();
    await screen.findByText('Burnley');

    const celule = within(randuri()[0]!).getAllByRole('cell').map((c) => c.textContent);
    expect(celule.slice(2, 9)).toEqual(['12', '8', '2', '2', '25:10', '+15', '26']);
    expect(within(randuri()[2]!).getByText('-12')).toBeInTheDocument();
  });

  test('valorile lipsa apar ca „—", nu null/NaN', async () => {
    mockGetClasament.mockResolvedValue(
      clasamentDetaliat({
        grupe: [{ nume: null, randuri: [randClasamentDetaliat({ jucate: null, golaveraj: null, puncte: null, golMarcate: null })] }],
      }),
    );
    const { container } = randeaza();
    await screen.findByText('Manchester United');
    expect(container.textContent).not.toMatch(/null|NaN|undefined/);
    expect(within(randuri()[0]!).getByText('—:10')).toBeInTheDocument();
  });

  test('echipele meciului sunt evidentiate, celelalte nu', async () => {
    mockGetClasament.mockResolvedValue(clasamentDetaliat());
    randeaza(33, 40);
    await screen.findByText('Burnley');
    const [mu, liv, bur] = randuri();
    expect(mu).toHaveClass('bg-primary/10');
    expect(liv).toHaveClass('bg-primary/10');
    expect(bur).not.toHaveClass('bg-primary/10');
  });

  test('legenda zonelor: retrogradarea e rosie, restul din paleta', async () => {
    mockGetClasament.mockResolvedValue(clasamentDetaliat());
    randeaza();
    await screen.findByText('Burnley');

    const legenda = screen.getByRole('list');
    expect(within(legenda).getByText('Relegation').querySelector('span')).toHaveClass('bg-red-600');
    expect(within(legenda).getByText('Champions League').querySelector('span')).toHaveClass('bg-blue-600');
    expect(within(randuri()[0]!).getByTitle('Champions League')).toHaveClass('bg-blue-600');
  });

  test('forma: chip cu link spre meci si tooltip cu adversarul si scorul', async () => {
    mockGetClasament.mockResolvedValue(
      clasamentDetaliat({
        grupe: [{
          nume: null,
          randuri: [randClasamentDetaliat({
            forma: [
              meciFormaClasament({ fixtureId: 7, rezultat: 'I', acasa: false, adversar: 'Arsenal', golGazde: 3, golOaspeti: 0 }),
              meciFormaClasament({ fixtureId: 8, rezultat: 'E', golGazde: 1, golOaspeti: 1 }),
            ],
          })],
        }],
      }),
    );
    randeaza();
    const chip = await screen.findByRole('link', { name: 'I' });
    expect(chip).toHaveAttribute('href', '/meci/7/centru');
    expect(chip.getAttribute('title')).toMatch(/la Arsenal · 3-0$/);
    expect(chip).toHaveClass('bg-accent');
    expect(screen.getByRole('link', { name: 'E' }).getAttribute('title')).toMatch(/vs Chelsea · 1-1$/);
  });

  test('cu grupe: se deschide grupa meciului, iar selectorul schimba grupa', async () => {
    const user = userEvent.setup();
    mockGetClasament.mockResolvedValue({
      grupe: [
        { nume: 'League A, Group 1', randuri: [randClasamentDetaliat({ teamId: 1, nume: 'Spania' })] },
        { nume: 'League B, Group 1', randuri: [randClasamentDetaliat({ teamId: 33, nume: 'Romania' })] },
      ],
    });
    randeaza(33, 99);

    const select = await screen.findByRole('combobox', { name: 'Grupa' });
    expect(select).toHaveDisplayValue('League B, Group 1');
    expect(screen.getByText('Romania')).toBeInTheDocument();
    expect(screen.queryByText('Spania')).toBeNull();

    await user.selectOptions(select, 'League A, Group 1');
    expect(screen.getByText('Spania')).toBeInTheDocument();
    expect(screen.queryByText('Romania')).toBeNull();
  });

  test('cu grupe dar echipele nu se gasesc: prima grupa', async () => {
    mockGetClasament.mockResolvedValue({
      grupe: [
        { nume: 'Grupa A', randuri: [randClasamentDetaliat({ teamId: 1, nume: 'Alfa' })] },
        { nume: 'Grupa B', randuri: [randClasamentDetaliat({ teamId: 2, nume: 'Beta' })] },
      ],
    });
    randeaza(500, 501);
    expect(await screen.findByRole('combobox', { name: 'Grupa' })).toHaveDisplayValue('Grupa A');
  });

  test('fara clasament publicat: mesaj „Fără clasament"', async () => {
    mockGetClasament.mockResolvedValue({ grupe: [] });
    randeaza();
    expect(await screen.findByText('Fără clasament')).toBeInTheDocument();
  });

  test('eroare API: „Clasament indisponibil"', async () => {
    mockGetClasament.mockRejectedValue(new Error('500'));
    randeaza();
    expect(await screen.findByText('Clasament indisponibil')).toBeInTheDocument();
  });
});
