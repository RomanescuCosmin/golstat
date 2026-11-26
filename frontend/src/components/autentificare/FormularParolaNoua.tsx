import { useState, type FormEvent } from 'react';
import { resetareParola, schimbaParola } from '../../api/auth';
import { useAuth } from '../../auth/AuthProvider';
import { ButonLink, ButonTrimite, CampParola, MesajEroare, MesajSucces, PAROLA_MINIMA, mesajEroare } from './campuri';

/**
 * Parola noua, in doua variante: `resetare` (din linkul de email, cu token) si `schimbare`
 * (utilizator autentificat, cere parola curenta).
 */
export function FormularParolaNoua(props: { varianta: 'resetare'; token: string } | { varianta: 'schimbare' }) {
  const { deschideModal, inchideModal, reincarca } = useAuth();
  const [curenta, setCurenta] = useState('');
  const [noua, setNoua] = useState('');
  const [confirmare, setConfirmare] = useState('');
  const [seTrimite, setSeTrimite] = useState(false);
  const [eroare, setEroare] = useState<string | null>(null);
  const [gata, setGata] = useState(false);

  async function trimite(e: FormEvent) {
    e.preventDefault();
    setEroare(null);
    if (noua !== confirmare) {
      setEroare('Parolele nu coincid.');
      return;
    }
    setSeTrimite(true);
    try {
      if (props.varianta === 'resetare') {
        await resetareParola(props.token, noua);
        await reincarca();
      } else {
        await schimbaParola(curenta, noua);
      }
      setGata(true);
    } catch (err) {
      setEroare(mesajEroare(err));
    } finally {
      setSeTrimite(false);
    }
  }

  if (gata) {
    return props.varianta === 'resetare' ? (
      <div className="space-y-4">
        <MesajSucces>Parola a fost schimbată și toate sesiunile au fost deconectate.</MesajSucces>
        <ButonLink onClick={() => deschideModal('autentificare')}>Autentifică-te cu parola nouă</ButonLink>
      </div>
    ) : (
      <div className="space-y-4">
        <MesajSucces>Parola a fost schimbată. Celelalte sesiuni au fost deconectate.</MesajSucces>
        <ButonLink onClick={inchideModal}>Închide</ButonLink>
      </div>
    );
  }

  return (
    <form onSubmit={trimite} className="space-y-4" noValidate>
      {props.varianta === 'schimbare' && (
        <CampParola
          eticheta="Parola curentă"
          autoComplete="current-password"
          required
          value={curenta}
          onChange={(e) => setCurenta(e.target.value)}
        />
      )}
      <CampParola
        eticheta="Parola nouă"
        autoComplete="new-password"
        required
        minLength={PAROLA_MINIMA}
        value={noua}
        ajutor={`Cel puțin ${PAROLA_MINIMA} caractere.`}
        onChange={(e) => setNoua(e.target.value)}
      />
      <CampParola
        eticheta="Confirmă parola nouă"
        autoComplete="new-password"
        required
        value={confirmare}
        onChange={(e) => setConfirmare(e.target.value)}
      />
      {eroare && <MesajEroare>{eroare}</MesajEroare>}
      <ButonTrimite seTrimite={seTrimite}>Salvează parola</ButonTrimite>
    </form>
  );
}
