import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import * as api from '../../api/auth';
import { ApiError } from '../../api/client';
import { AuthProvider, useAuth, type ModAutentificare } from '../../auth/AuthProvider';
import type { LinkEmail } from '../../lib/linkEmail';
import { AutentificareModal } from './AutentificareModal';

vi.mock('../../api/auth', () => ({
  getEu: vi.fn(),
  autentificare: vi.fn(),
  deconectare: vi.fn(),
  inregistrare: vi.fn(),
  retrimiteConfirmare: vi.fn(),
  parolaUitata: vi.fn(),
  resetareParola: vi.fn(),
  confirmareEmail: vi.fn(),
  schimbaParola: vi.fn(),
}));

const PAROLA = 'cal verde sare gardul';

function Deschide({ mod }: { mod: ModAutentificare }) {
  const { deschideModal } = useAuth();
  return <button onClick={() => deschideModal(mod)}>deschide</button>;
}

async function randeaza(mod: ModAutentificare = 'autentificare', linkEmail: LinkEmail = null) {
  const user = userEvent.setup();
  render(
    <AuthProvider linkEmail={linkEmail}>
      <Deschide mod={mod} />
      <AutentificareModal />
    </AuthProvider>,
  );
  if (!linkEmail) await user.click(screen.getByText('deschide'));
  return user;
}

describe('AutentificareModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.getEu).mockResolvedValue(null);
  });

  test('taburile comuta intre autentificare si cont nou', async () => {
    const user = await randeaza();
    expect(screen.getByRole('dialog', { name: 'Intră în cont' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cont nou' }));

    expect(screen.getByRole('dialog', { name: 'Creează un cont' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nume')).toBeInTheDocument();
  });

  test('autentificare reusita inchide fereastra', async () => {
    vi.mocked(api.autentificare).mockResolvedValue({ id: 7, email: 'ana@ex.ro', nume: 'Ana' });
    const user = await randeaza();

    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));

    expect(api.autentificare).toHaveBeenCalledWith('ana@ex.ro', PAROLA);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  test('401: mesaj generic si parola stearsa din camp', async () => {
    vi.mocked(api.autentificare).mockRejectedValue(new ApiError(401, 'Autentificare eșuată', 'Email sau parolă incorecte.'));
    const user = await randeaza();

    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Email sau parolă incorecte.');
    expect(screen.getByLabelText('Parolă')).toHaveValue('');
  });

  test('403 email neconfirmat: buton de retrimitere', async () => {
    vi.mocked(api.autentificare).mockRejectedValue(
      new ApiError(403, 'Email neconfirmat', 'Confirmă adresa de email înainte să te autentifici.', { cod: 'email-neconfirmat' }),
    );
    vi.mocked(api.retrimiteConfirmare).mockResolvedValue(undefined);
    const user = await randeaza();

    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));
    await user.click(await screen.findByRole('button', { name: 'Retrimite emailul de confirmare' }));

    expect(api.retrimiteConfirmare).toHaveBeenCalledWith('ana@ex.ro');
    expect(await screen.findByRole('status')).toHaveTextContent('email nou de confirmare');
  });

  test('429: arata minutele ramase', async () => {
    vi.mocked(api.autentificare).mockRejectedValue(new ApiError(429, 'Prea multe', null, { retryAfter: 600 }));
    const user = await randeaza();

    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Reîncearcă în 10 minute.');
  });

  test('cont nou: parole diferite → eroare locala, fara cerere', async () => {
    const user = await randeaza('inregistrare');

    await user.type(screen.getByLabelText('Nume'), 'Ana');
    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola'), `${PAROLA}x`);
    await user.click(screen.getByRole('button', { name: 'Creează cont' }));

    expect(screen.getByText('Parolele nu coincid.')).toBeInTheDocument();
    expect(api.inregistrare).not.toHaveBeenCalled();
  });

  test('cont nou pe email existent: mesaj clar cu drum spre autentificare si parola uitata', async () => {
    vi.mocked(api.inregistrare).mockRejectedValue(
      new ApiError(409, 'Email folosit', 'Există deja un cont cu acest email.', { cod: 'email-existent' }),
    );
    const user = await randeaza('inregistrare');

    await user.type(screen.getByLabelText('Nume'), 'Ana');
    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Creează cont' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Există deja un cont cu adresa ana@ex.ro.');
    expect(screen.getByRole('button', { name: 'Ai uitat parola?' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));
    expect(screen.getByRole('dialog', { name: 'Intră în cont' })).toBeInTheDocument();
  });

  test('cont nou pe email existent: „Folosește altă adresă” revine la formular cu datele pastrate', async () => {
    vi.mocked(api.inregistrare).mockRejectedValue(
      new ApiError(409, 'Email folosit', 'Există deja un cont cu acest email.', { cod: 'email-existent' }),
    );
    const user = await randeaza('inregistrare');

    await user.type(screen.getByLabelText('Nume'), 'Ana');
    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Creează cont' }));
    await user.click(await screen.findByRole('button', { name: 'Folosește altă adresă' }));

    expect(screen.getByLabelText('Email')).toHaveValue('ana@ex.ro');
    expect(screen.getByLabelText('Nume')).toHaveValue('Ana');
  });

  test('cont nou reusit: ecranul „verifica emailul”', async () => {
    vi.mocked(api.inregistrare).mockResolvedValue(undefined);
    const user = await randeaza('inregistrare');

    await user.type(screen.getByLabelText('Nume'), 'Ana');
    await user.type(screen.getByLabelText('Email'), 'ana@ex.ro');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Creează cont' }));

    expect(api.inregistrare).toHaveBeenCalledWith('ana@ex.ro', PAROLA, 'Ana');
    expect(await screen.findByRole('status')).toHaveTextContent('Verifică-ți emailul.');
    expect(screen.getByText(/Ți-am trimis un link de confirmare/)).toBeInTheDocument();
  });

  test('cont nou: erorile de validare de la server apar sub campuri', async () => {
    vi.mocked(api.inregistrare).mockRejectedValue(
      new ApiError(400, 'Date invalide', 'Verifică câmpurile marcate.', { erori: { email: 'Adresă de email invalidă.' } }),
    );
    const user = await randeaza('inregistrare');

    await user.type(screen.getByLabelText('Nume'), 'Ana');
    await user.type(screen.getByLabelText('Email'), 'x@y');
    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Creează cont' }));

    expect(await screen.findByText('Adresă de email invalidă.')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
  });

  test('parola uitata: acelasi mesaj indiferent de adresa', async () => {
    vi.mocked(api.parolaUitata).mockResolvedValue(undefined);
    const user = await randeaza();

    await user.click(screen.getByRole('button', { name: 'Ai uitat parola?' }));
    await user.type(screen.getByLabelText('Email'), 'oricine@ex.ro');
    await user.click(screen.getByRole('button', { name: 'Trimite linkul' }));

    expect(api.parolaUitata).toHaveBeenCalledWith('oricine@ex.ro');
    expect(await screen.findByRole('status')).toHaveTextContent('Dacă există un cont cu această adresă');
  });

  test('link de resetare din email: deschide direct formularul cu tokenul', async () => {
    vi.mocked(api.resetareParola).mockResolvedValue(undefined);
    const user = await randeaza('resetare', { tip: 'resetare', token: 'TOK' });

    expect(screen.getByRole('dialog', { name: 'Alege o parolă nouă' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('Parola nouă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola nouă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Salvează parola' }));

    expect(api.resetareParola).toHaveBeenCalledWith('TOK', PAROLA);
    expect(await screen.findByRole('status')).toHaveTextContent('toate sesiunile au fost deconectate');
  });

  test('resetare: parole diferite nu trimit cererea', async () => {
    const user = await randeaza('resetare', { tip: 'resetare', token: 'TOK' });

    await user.type(screen.getByLabelText('Parola nouă'), PAROLA);
    await user.type(screen.getByLabelText('Confirmă parola nouă'), 'altceva altceva');
    await user.click(screen.getByRole('button', { name: 'Salvează parola' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Parolele nu coincid.');
    expect(api.resetareParola).not.toHaveBeenCalled();
  });

  test('link de confirmare: cere parola contului si o trimite cu tokenul', async () => {
    vi.mocked(api.confirmareEmail).mockResolvedValue(undefined);
    const user = await randeaza('confirmare', { tip: 'confirmare', token: 'TOK' });

    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Confirmă emailul' }));

    expect(api.confirmareEmail).toHaveBeenCalledWith('TOK', PAROLA);
    expect(await screen.findByRole('status')).toHaveTextContent('Adresa de email a fost confirmată.');
  });

  test('link de confirmare expirat/folosit: explica si duce la autentificare', async () => {
    vi.mocked(api.confirmareEmail).mockRejectedValue(
      new ApiError(400, 'Link invalid', 'Link invalid sau expirat.', { cod: 'token-invalid' }),
    );
    const user = await randeaza('confirmare', { tip: 'confirmare', token: 'TOK' });

    await user.type(screen.getByLabelText('Parolă'), PAROLA);
    await user.click(screen.getByRole('button', { name: 'Confirmă emailul' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('expirat sau a fost deja folosit');
    await user.click(screen.getByRole('button', { name: 'Mergi la autentificare' }));
    expect(screen.getByRole('dialog', { name: 'Intră în cont' })).toBeInTheDocument();
  });

  test('confirmare cu parola gresita: ramane pe formular cu mesaj', async () => {
    vi.mocked(api.confirmareEmail).mockRejectedValue(
      new ApiError(400, 'Parolă greșită', 'Parola curentă e greșită.', { cod: 'parola-gresita' }),
    );
    const user = await randeaza('confirmare', { tip: 'confirmare', token: 'TOK' });

    await user.type(screen.getByLabelText('Parolă'), 'gresita');
    await user.click(screen.getByRole('button', { name: 'Confirmă emailul' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Parola curentă e greșită.');
    expect(screen.getByRole('button', { name: 'Confirmă emailul' })).toBeInTheDocument();
  });

  test('schimbare parola: trimite parola curenta si pe cea noua', async () => {
    vi.mocked(api.schimbaParola).mockResolvedValue(undefined);
    const user = await randeaza('schimbare-parola');

    await user.type(screen.getByLabelText('Parola curentă'), PAROLA);
    await user.type(screen.getByLabelText('Parola nouă'), 'minge rosie pe stadion');
    await user.type(screen.getByLabelText('Confirmă parola nouă'), 'minge rosie pe stadion');
    await user.click(screen.getByRole('button', { name: 'Salvează parola' }));

    expect(api.schimbaParola).toHaveBeenCalledWith(PAROLA, 'minge rosie pe stadion');
  });

  test('butonul Arată/Ascunde comuta tipul campului de parola', async () => {
    const user = await randeaza();
    const parola = screen.getByLabelText('Parolă');
    expect(parola).toHaveAttribute('type', 'password');
    expect(parola).toHaveAttribute('autocomplete', 'current-password');

    await user.click(screen.getByRole('button', { name: 'Arată parola' }));
    expect(screen.getByRole('button', { name: 'Arată parola' })).toHaveAttribute('aria-pressed', 'true');

    expect(parola).toHaveAttribute('type', 'text');
  });
});
