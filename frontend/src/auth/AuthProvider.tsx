import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as api from '../api/auth';
import type { Utilizator } from '../api/auth';
import { citesteSiCurataLinkEmail, type LinkEmail } from '../lib/linkEmail';

export type ModAutentificare =
  | 'autentificare'
  | 'inregistrare'
  | 'parola-uitata'
  | 'resetare'
  | 'confirmare'
  | 'schimbare-parola';

export interface StareModal {
  mod: ModAutentificare;
  /** Tokenul din linkul de email (doar pentru `resetare` / `confirmare`). */
  token?: string;
}

interface AuthValue {
  stare: 'incarcare' | 'anonim' | 'autentificat';
  utilizator: Utilizator | null;
  autentifica: (email: string, parola: string) => Promise<void>;
  deconecteaza: () => Promise<void>;
  /** Reciteste sesiunea de pe server (ex. dupa o resetare de parola care a deconectat totul). */
  reincarca: () => Promise<void>;
  modal: StareModal | null;
  deschideModal: (mod: ModAutentificare, token?: string) => void;
  inchideModal: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children, linkEmail = null }: { children: ReactNode; linkEmail?: LinkEmail }) {
  const [stare, setStare] = useState<AuthValue['stare']>('incarcare');
  const [utilizator, setUtilizator] = useState<Utilizator | null>(null);
  const [modal, setModal] = useState<StareModal | null>(linkEmail && { mod: linkEmail.tip, token: linkEmail.token });

  const seteazaUtilizator = useCallback((u: Utilizator | null) => {
    setUtilizator(u);
    setStare(u ? 'autentificat' : 'anonim');
  }, []);

  useEffect(() => {
    let activ = true;
    api
      .getEu()
      .then((u) => activ && seteazaUtilizator(u))
      .catch(() => activ && seteazaUtilizator(null));
    return () => {
      activ = false;
    };
  }, [seteazaUtilizator]);

  // link din email lipit intr-un tab deja deschis: se schimba doar fragmentul, pagina nu se reincarca
  useEffect(() => {
    const laSchimbareFragment = () => {
      const link = citesteSiCurataLinkEmail();
      if (link) setModal({ mod: link.tip, token: link.token });
    };
    window.addEventListener('hashchange', laSchimbareFragment);
    return () => window.removeEventListener('hashchange', laSchimbareFragment);
  }, []);

  const reincarca = useCallback(async () => {
    seteazaUtilizator(await api.getEu().catch(() => null));
  }, [seteazaUtilizator]);

  const autentifica = useCallback(
    async (email: string, parola: string) => seteazaUtilizator(await api.autentificare(email, parola)),
    [seteazaUtilizator],
  );

  const deconecteaza = useCallback(async () => {
    try {
      await api.deconectare();
    } finally {
      seteazaUtilizator(null);
    }
  }, [seteazaUtilizator]);

  const deschideModal = useCallback((mod: ModAutentificare, token?: string) => setModal({ mod, token }), []);
  const inchideModal = useCallback(() => setModal(null), []);

  const valoare = useMemo(
    () => ({ stare, utilizator, autentifica, deconecteaza, reincarca, modal, deschideModal, inchideModal }),
    [stare, utilizator, autentifica, deconecteaza, reincarca, modal, deschideModal, inchideModal],
  );

  return <AuthContext.Provider value={valoare}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth trebuie folosit în interiorul unui AuthProvider');
  }
  return ctx;
}
