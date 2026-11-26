import { useEffect, useRef, useState, type FormEvent } from 'react';
import { inregistrare } from '../../api/auth';
import { ApiError } from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import {
  ButonLink,
  ButonTrimite,
  Camp,
  CampParola,
  MesajEroare,
  MesajSucces,
  PAROLA_MINIMA,
  eroriCampuri,
  mesajEroare,
} from './campuri';

const CAMPURI = ['nume', 'email', 'parola', 'confirmare'];

export function FormularInregistrare() {
  const [nume, setNume] = useState('');
  const [email, setEmail] = useState('');
  const [parola, setParola] = useState('');
  const [confirmare, setConfirmare] = useState('');
  const [seTrimite, setSeTrimite] = useState(false);
  const [eroare, setEroare] = useState<string | null>(null);
  const [erori, setErori] = useState<Record<string, string>>({});
  const [trimis, setTrimis] = useState(false);
  const [emailExistent, setEmailExistent] = useState(false);
  const { deschideModal } = useAuth();
  const formular = useRef<HTMLFormElement>(null);

  useEffect(() => {
    formular.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  }, [erori]);

  async function trimite(e: FormEvent) {
    e.preventDefault();
    setEroare(null);
    setErori({});
    if (parola !== confirmare) {
      setErori({ confirmare: 'Parolele nu coincid.' });
      return;
    }
    setSeTrimite(true);
    try {
      await inregistrare(email, parola, nume);
      setTrimis(true);
    } catch (err) {
      setEmailExistent(err instanceof ApiError && err.cod === 'email-existent');
      setErori(
        err instanceof ApiError && err.cod === 'parola-slaba' ? { parola: mesajEroare(err) } : eroriCampuri(err),
      );
      setEroare(mesajEroare(err));
    } finally {
      setSeTrimite(false);
    }
  }

  if (trimis) {
    return (
      <div className="space-y-3">
        <MesajSucces>Verifică-ți emailul.</MesajSucces>
        <p className="text-sm text-ink2">
          Ți-am trimis un link de confirmare la <span className="font-semibold text-ink">{email}</span>. Linkul e
          valabil 24 de ore; la confirmare îți vom cere parola aleasă acum.
        </p>
      </div>
    );
  }

  if (emailExistent) {
    return (
      <div className="space-y-4">
        <MesajEroare>Există deja un cont cu adresa {email}.</MesajEroare>
        <div className="flex flex-col items-center gap-2">
          <ButonLink onClick={() => deschideModal('autentificare')}>Intră în cont</ButonLink>
          <ButonLink onClick={() => deschideModal('parola-uitata')}>Ai uitat parola?</ButonLink>
          <ButonLink onClick={() => setEmailExistent(false)}>Folosește altă adresă</ButonLink>
        </div>
      </div>
    );
  }

  return (
    <form ref={formular} onSubmit={trimite} className="space-y-4" noValidate>
      <Camp
        eticheta="Nume"
        autoComplete="name"
        required
        minLength={2}
        maxLength={50}
        value={nume}
        eroare={erori.nume}
        onChange={(e) => setNume(e.target.value)}
      />
      <Camp
        eticheta="Email"
        type="email"
        autoComplete="email"
        required
        maxLength={254}
        value={email}
        eroare={erori.email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <CampParola
        eticheta="Parolă"
        autoComplete="new-password"
        required
        minLength={PAROLA_MINIMA}
        value={parola}
        eroare={erori.parola}
        ajutor={`Cel puțin ${PAROLA_MINIMA} caractere. O frază din câteva cuvinte e ușor de ținut minte.`}
        onChange={(e) => setParola(e.target.value)}
      />
      <CampParola
        eticheta="Confirmă parola"
        autoComplete="new-password"
        required
        value={confirmare}
        eroare={erori.confirmare}
        onChange={(e) => setConfirmare(e.target.value)}
      />
      {eroare && !CAMPURI.some((c) => erori[c]) && <MesajEroare>{eroare}</MesajEroare>}
      <ButonTrimite seTrimite={seTrimite}>Creează cont</ButonTrimite>
    </form>
  );
}
