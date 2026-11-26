import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import { ThemeProvider } from '../../theme/ThemeProvider';
import { BaraNavigareJos } from './BaraNavigareJos';

function Ruta() {
  return <p data-testid="ruta">{useLocation().pathname}</p>;
}

function randeaza(ruta: string) {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <BaraNavigareJos />
        <Routes>
          <Route path="*" element={<Ruta />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('BaraNavigareJos', () => {
  test('sectiunea curenta e marcata activa', () => {
    randeaza('/live');
    expect(screen.getByRole('link', { name: 'Live' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Meciuri' })).not.toHaveAttribute('aria-current');
  });

  test('„Mai mult" deschide restul sectiunilor si navigheaza, apoi se inchide', async () => {
    const user = userEvent.setup();
    randeaza('/');
    await user.click(screen.getByRole('button', { name: 'Mai mult' }));

    const meniu = screen.getByRole('dialog', { name: 'Mai mult' });
    expect(meniu).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Statistici' }));

    expect(screen.getByTestId('ruta')).toHaveTextContent('/statistici');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  test('pe o sectiune din meniu, „Mai mult" apare activ', () => {
    randeaza('/echipa/33');
    expect(screen.getByRole('button', { name: 'Mai mult' })).toHaveClass('text-primary');
  });
});
