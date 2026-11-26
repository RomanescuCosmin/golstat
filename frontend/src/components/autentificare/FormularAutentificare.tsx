import { useState, type FormEvent } from 'react';
import { retrimiteConfirmare } from '../../api/auth';
import { ApiError } from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import { ButonLink, ButonTrimite, Camp, CampParola, MesajEroare, MesajSucces, mesajEroare } from './campuri';

export function FormularAutentificare() {
  const { autentifica, inchideModal, deschideModal } = useAuth();
  const [email, setEmail] = useState('');
  const [parola, setParola] = useState('');
  const [seTrimite, setSeTrimite] = useState(false);
  const [eroare, setEroare] = useState<string | null>(null);
  const [neconfirmat, setNeconfirmat] = useState(false);
  const [retrimis, setRetrimis] = useState(false);
  const [seRetrimite, setSeRetrimite] = useState(false);

  async function trimite(e: FormEvent) {
    e.preventDefault();
    setSeTrimite(true);
    setEroare(null);
    setNeconfirmat(false);
    try {
      await autentifica(email, parola);
      inchideModal();
    } catch (err) {
      setNeconfirmat(err instanceof ApiError && err.cod === 'email-neconfirmat');
      setEroare(mesajEroare(err));
      setParola('');
    } finally {
      setSeTrimite(false);
    }
  }

  async function retrimite() {
    if (seRetrimite) return;
    setSeRetrimite(true);
    try {
      await retrimiteConfirmare(email);
      setRetrimis(true);
    } catch (err) {
      setEroare(mesajEroare(err));
    } finally {
      setSeRetrimite(false);
    }
  }

  return (
    <form onSubmit={trimite} className="space-y-4" noValidate>
      <Camp
        eticheta="Email"
        type="email"
        autoComplete="username"
        required
        maxLength={254}
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <CampParola
        eticheta="Parolă"
        autoComplete="current-password"
        required
        value={parola}
        onChange={(e) => setParola(e.target.value)}
      />
      {eroare && <MesajEroare>{eroare}</MesajEroare>}
      {neconfirmat &&
        (retrimis ? (
          <MesajSucces>Ți-am trimis un email nou de confirmare.</MesajSucces>
        ) : (
          <ButonLink onClick={retrimite}>Retrimite emailul de confirmare</ButonLink>
        ))}
      <ButonTrimite seTrimite={seTrimite}>Intră în cont</ButonTrimite>
      <div className="text-center">
        <ButonLink onClick={() => deschideModal('parola-uitata')}>Ai uitat parola?</ButonLink>
      </div>
    </form>
  );
}
