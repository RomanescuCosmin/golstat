import { act, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import * as api from '../api/auth';
import { AuthProvider, useAuth } from './AuthProvider';

vi.mock('../api/auth', () => ({
  getEu: vi.fn(),
  autentificare: vi.fn(),
  deconectare: vi.fn(),
}));

const ANA = { id: 7, email: 'ana@ex.ro', nume: 'Ana' };

let auth: ReturnType<typeof useAuth>;
function Sonda() {
  auth = useAuth();
  return (
    <p data-testid="stare">
      {auth.stare}|{auth.utilizator?.email ?? '-'}|{auth.modal ? `${auth.modal.mod}:${auth.modal.token ?? ''}` : '-'}
    </p>
  );
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.mocked(api.getEu).mockReset();
    vi.mocked(api.autentificare).mockReset();
    vi.mocked(api.deconectare).mockReset();
  });

  test('vizitator: incarcare → anonim', async () => {
    vi.mocked(api.getEu).mockResolvedValue(null);
    render(<AuthProvider><Sonda /></AuthProvider>);

    expect(screen.getByTestId('stare')).toHaveTextContent('incarcare|-|-');
    await waitFor(() => expect(screen.getByTestId('stare')).toHaveTextContent('anonim|-|-'));
  });

  test('sesiune existenta: autentificat', async () => {
    vi.mocked(api.getEu).mockResolvedValue(ANA);
    render(<AuthProvider><Sonda /></AuthProvider>);

    await waitFor(() => expect(screen.getByTestId('stare')).toHaveTextContent('autentificat|ana@ex.ro'));
  });

  test('eroare la /eu: tratat ca vizitator', async () => {
    vi.mocked(api.getEu).mockRejectedValue(new Error('jos'));
    render(<AuthProvider><Sonda /></AuthProvider>);

    await waitFor(() => expect(screen.getByTestId('stare')).toHaveTextContent('anonim'));
  });

  test('autentifica si deconecteaza', async () => {
    vi.mocked(api.getEu).mockResolvedValue(null);
    vi.mocked(api.autentificare).mockResolvedValue(ANA);
    vi.mocked(api.deconectare).mockResolvedValue(undefined);
    render(<AuthProvider><Sonda /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('stare')).toHaveTextContent('anonim'));

    await act(() => auth.autentifica('ana@ex.ro', 'parola'));
    expect(screen.getByTestId('stare')).toHaveTextContent('autentificat|ana@ex.ro');

    await act(() => auth.deconecteaza());
    expect(screen.getByTestId('stare')).toHaveTextContent('anonim|-');
  });

  test('deconectarea esuata pe server tot curata starea locala', async () => {
    vi.mocked(api.getEu).mockResolvedValue(ANA);
    vi.mocked(api.deconectare).mockRejectedValue(new Error('jos'));
    render(<AuthProvider><Sonda /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('stare')).toHaveTextContent('autentificat'));

    await act(() => auth.deconecteaza().catch(() => undefined));

    expect(screen.getByTestId('stare')).toHaveTextContent('anonim|-');
  });

  test('linkul din email deschide modala in modul potrivit, cu token', async () => {
    vi.mocked(api.getEu).mockResolvedValue(null);
    render(
      <AuthProvider linkEmail={{ tip: 'resetare', token: 'TOK' }}>
        <Sonda />
      </AuthProvider>,
    );

    expect(screen.getByTestId('stare')).toHaveTextContent('resetare:TOK');
    act(() => auth.inchideModal());
    expect(screen.getByTestId('stare')).toHaveTextContent('|-');
  });

  test('useAuth in afara providerului arunca', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Sonda />)).toThrow(/AuthProvider/);
  });
});
