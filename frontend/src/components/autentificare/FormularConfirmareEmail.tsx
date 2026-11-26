import { useState, type FormEvent } from 'react';
import { confirmareEmail } from '../../api/auth';
import { ApiError } from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import { ButonLink, ButonTrimite, CampParola, MesajEroare, MesajSucces, mesajEroare } from './campuri';

/**
 * Confirmarea cere parola contului: un cont creat de altcineva cu adresa ta nu poate fi activat
 * doar apasand linkul. Daca nu-ti recunosti contul, „Parolă uitată” iti da controlul asupra lui.
 */
export function FormularConfirmareEmail({ token }: { token: string }) {
  const { deschideModal } = useAuth();
  const [parola, setParola] = useState('');
  const [seTrimite, setSeTrimite] = useState(false);
  const [eroare, setEroare] = useState<string | null>(null);
  const [confirmat, setConfirmat] = useState(false);
  const [linkInvalid, setLinkInvalid] = useState(false);

  async function trimite(e: FormEvent) {
    e.preventDefault();
    setSeTrimite(true);
    setEroare(null);
    try {
      await confirmareEmail(token, parola);
      setConfirmat(true);
    } catch (err) {
      setLinkInvalid(err instanceof ApiError && err.cod === 'token-invalid');
      setEroare(mesajEroare(err));
      setParola('');
    } finally {
      setSeTrimite(false);
    }
  }

  if (linkInvalid) {
    return (
      <div className="space-y-4">
        <MesajEroare>Linkul a expirat sau a fost deja folosit.</MesajEroare>
        <p className="text-sm text-ink2">
          Dacă ai primit mai multe emailuri de confirmare, poți folosi oricare dintre cele din ultimele 24 de ore.
          Altfel, autentifică-te: vei putea cere un link nou.
        </p>
        <ButonLink onClick={() => deschideModal('autentificare')}>Mergi la autentificare</ButonLink>
      </div>
    );
  }

  if (confirmat) {
    return (
      <div className="space-y-4">
        <MesajSucces>Adresa de email a fost confirmată.</MesajSucces>
        <ButonLink onClick={() => deschideModal('autentificare')}>Autentifică-te</ButonLink>
      </div>
    );
  }

  return (
    <form onSubmit={trimite} className="space-y-4" noValidate>
      <p className="text-sm text-ink2">Introdu parola aleasă la crearea contului ca să confirmi adresa de email.</p>
      <CampParola
        eticheta="Parolă"
        autoComplete="current-password"
        required
        value={parola}
        onChange={(e) => setParola(e.target.value)}
      />
      {eroare && <MesajEroare>{eroare}</MesajEroare>}
      <ButonTrimite seTrimite={seTrimite}>Confirmă emailul</ButonTrimite>
      <p className="text-center text-xs text-ink2">
        Nu ai creat tu contul sau nu-ți amintești parola?{' '}
        <ButonLink onClick={() => deschideModal('parola-uitata')}>Setează o parolă nouă</ButonLink>
      </p>
    </form>
  );
}
