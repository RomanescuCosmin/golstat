import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import * as api from '../../api/auth';
import { AuthProvider } from '../../auth/AuthProvider';
import { AutentificareModal } from '../autentificare/AutentificareModal';
import { ContUtilizator } from './ContUtilizator';

vi.mock('../../api/auth', () => ({
  getEu: vi.fn(),
  autentificare: vi.fn(),
  deconectare: vi.fn(),
}));

function randeaza() {
  return render(
    <AuthProvider>
      <MemoryRouter>
        <ContUtilizator cuNume />
      </MemoryRouter>
      <AutentificareModal />
    </AuthProvider>,
  );
}

describe('ContUtilizator', () => {
  beforeEach(() => vi.clearAllMocks());

  test('vizitator: butonul Autentificare deschide fereastra modala', async () => {
    vi.mocked(api.getEu).mockResolvedValue(null);
    const user = userEvent.setup();
    randeaza();

    await user.click(await screen.findByRole('button', { name: 'Autentificare' }));

    expect(screen.getByRole('dialog', { name: 'Intră în cont' })).toBeInTheDocument();
  });

  test('autentificat: initiala si numele, meniul arata emailul', async () => {
    vi.mocked(api.getEu).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'ana Pop' });
    const user = userEvent.setup();
    randeaza();

    const buton = await screen.findByRole('button', { name: 'Contul tău: ana Pop' });
    expect(buton).toHaveTextContent('A');
    await user.click(buton);

    expect(screen.getByText('ana@ex.ro')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Profil' })).toHaveAttribute('href', '/profil');
  });

  test('Deconectare din meniu: cere logout si revine la butonul de autentificare', async () => {
    vi.mocked(api.getEu).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'Ana' });
    vi.mocked(api.deconectare).mockResolvedValue(undefined);
    const user = userEvent.setup();
    randeaza();

    await user.click(await screen.findByRole('button', { name: 'Contul tău: Ana' }));
    await user.click(screen.getByRole('button', { name: 'Deconectare' }));

    expect(api.deconectare).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Autentificare' })).toBeInTheDocument());
  });

  test('Schimbă parola din meniu deschide fereastra modala', async () => {
    vi.mocked(api.getEu).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'Ana' });
    const user = userEvent.setup();
    randeaza();

    await user.click(await screen.findByRole('button', { name: 'Contul tău: Ana' }));
    await user.click(screen.getByRole('button', { name: 'Schimbă parola' }));

    expect(screen.getByRole('dialog', { name: 'Schimbă parola' })).toBeInTheDocument();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  test('Escape inchide meniul', async () => {
    vi.mocked(api.getEu).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'Ana' });
    const user = userEvent.setup();
    randeaza();

    const avatar = await screen.findByRole('button', { name: 'Contul tău: Ana' });
    await user.click(avatar);
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('list')).not.toBeInTheDocument();
    expect(avatar).toHaveFocus();
    expect(avatar).toHaveAttribute('aria-expanded', 'false');
  });

  test('inchiderea ferestrei deschise din panou readuce focusul pe avatar', async () => {
    vi.mocked(api.getEu).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'Ana' });
    const user = userEvent.setup();
    document.body.innerHTML = '<div id="root"></div>';
    render(
      <AuthProvider>
        <MemoryRouter>
          <ContUtilizator />
          <AutentificareModal />
        </MemoryRouter>
      </AuthProvider>,
      { container: document.getElementById('root')! },
    );

    const avatar = await screen.findByRole('button', { name: 'Contul tău: Ana' });
    await user.click(avatar);
    await user.click(screen.getByRole('button', { name: 'Schimbă parola' }));
    expect(screen.getByLabelText('Parola curentă')).toHaveFocus();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(avatar).toHaveFocus();
  });

  test('deconectare esuata pe server: interfata trece oricum pe vizitator, fara eroare nesurprinsa', async () => {
    vi.mocked(api.getEu).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'Ana' });
    vi.mocked(api.deconectare).mockRejectedValue(new Error('jos'));
    const user = userEvent.setup();
    randeaza();

    await user.click(await screen.findByRole('button', { name: 'Contul tău: Ana' }));
    await user.click(screen.getByRole('button', { name: 'Deconectare' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Autentificare' })).toBeInTheDocument());
  });
});
