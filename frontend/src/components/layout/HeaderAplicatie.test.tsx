import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, test, vi } from 'vitest';
import { AuthProvider } from '../../auth/AuthProvider';
import { ThemeProvider } from '../../theme/ThemeProvider';
import { HeaderAplicatie } from './HeaderAplicatie';

vi.mock('../../api/auth', () => ({ getEu: vi.fn().mockResolvedValue(null) }));

function Ruta() {
  return <p data-testid="ruta">{useLocation().pathname}</p>;
}

function randeaza(ruta: string) {
  return render(
    <ThemeProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={[ruta]}>
          <HeaderAplicatie />
          <Routes>
            <Route path="*" element={<Ruta />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  );
}

describe('HeaderAplicatie', () => {
  test('logo-ul golstat (minge + text) duce acasa, si pe mobil si pe desktop', async () => {
    const user = userEvent.setup();
    randeaza('/piete');
    const logouri = screen.getAllByRole('link', { name: 'golstat — acasă' });
    expect(logouri).toHaveLength(2);
    for (const logo of logouri) expect(logo).toHaveAttribute('href', '/');

    await user.click(logouri[0]!);
    expect(screen.getByTestId('ruta')).toHaveTextContent(/^\/$/);
  });

  test('fara buton de notificari langa cont; vizitatorul vede butonul de autentificare', async () => {
    randeaza('/');
    expect(screen.queryByRole('button', { name: 'Notificări' })).toBeNull();
    expect((await screen.findAllByRole('button', { name: 'Autentificare' })).length).toBeGreaterThan(0);
  });
});
