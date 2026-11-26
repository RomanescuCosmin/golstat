import { useState, type FormEvent } from 'react';
import { parolaUitata } from '../../api/auth';
import { useAuth } from '../../auth/AuthProvider';
import { ButonLink, ButonTrimite, Camp, MesajEroare, MesajSucces, eroriCampuri, mesajEroare } from './campuri';

export function FormularParolaUitata() {
  const { deschideModal } = useAuth();
  const [email, setEmail] = useState('');
  const [seTrimite, setSeTrimite] = useState(false);
  const [eroare, setEroare] = useState<string | null>(null);
  const [trimis, setTrimis] = useState(false);

  async function trimite(e: FormEvent) {
    e.preventDefault();
    setSeTrimite(true);
    setEroare(null);
    try {
      await parolaUitata(email);
      setTrimis(true);
    } catch (err) {
      setEroare(eroriCampuri(err).email ?? mesajEroare(err));
    } finally {
      setSeTrimite(false);
    }
  }

  return (
    <div className="space-y-4">
      {trimis ? (
        <MesajSucces>
          Dacă există un cont cu această adresă, ți-am trimis un link de resetare. Linkul e valabil 30 de minute.
        </MesajSucces>
      ) : (
        <form onSubmit={trimite} className="space-y-4" noValidate>
          <p className="text-sm text-ink2">Introdu adresa contului și îți trimitem un link pentru o parolă nouă.</p>
          <Camp
            eticheta="Email"
            type="email"
            autoComplete="email"
            required
            maxLength={254}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          {eroare && <MesajEroare>{eroare}</MesajEroare>}
          <ButonTrimite seTrimite={seTrimite}>Trimite linkul</ButonTrimite>
        </form>
      )}
      <div className="text-center">
        <ButonLink onClick={() => deschideModal('autentificare')}>Înapoi la autentificare</ButonLink>
      </div>
    </div>
  );
}
