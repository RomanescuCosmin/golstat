import { useEffect, useId, useRef, useState, type FocusEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth, type ModAutentificare } from '../../auth/AuthProvider';
import { IconUser } from '../ui/icons';

const CLASE_ELEMENT = 'block w-full rounded-lg px-3 py-2 text-left text-sm text-ink transition hover:bg-ink2/10';

/**
 * Contul din header: buton „Autentificare” pentru vizitatori (deschide fereastra modala),
 * iar pentru utilizatorii conectati avatarul cu panoul contului (disclosure, nu meniu ARIA).
 */
export function ContUtilizator({ cuNume = false }: { cuNume?: boolean }) {
  const { stare, utilizator, deschideModal, deconecteaza } = useAuth();
  const [deschis, setDeschis] = useState(false);
  const radacina = useRef<HTMLDivElement>(null);
  const buton = useRef<HTMLButtonElement>(null);
  const idPanou = useId();

  useEffect(() => {
    if (!deschis) return;
    const laClick = (e: MouseEvent) => {
      if (!radacina.current?.contains(e.target as Node)) setDeschis(false);
    };
    const laTasta = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setDeschis(false);
        buton.current?.focus();
      }
    };
    document.addEventListener('mousedown', laClick);
    document.addEventListener('keydown', laTasta);
    return () => {
      document.removeEventListener('mousedown', laClick);
      document.removeEventListener('keydown', laTasta);
    };
  }, [deschis]);

  if (stare === 'incarcare') {
    return <span aria-hidden="true" className="block h-9 w-9 animate-pulse rounded-full bg-ink2/15" />;
  }

  if (!utilizator) {
    return (
      <button
        type="button"
        onClick={() => deschideModal('autentificare')}
        className="flex h-9 items-center gap-2 rounded-full bg-primary px-3 text-sm font-semibold text-white transition hover:bg-primary/90 sm:px-4"
      >
        <IconUser width={16} height={16} />
        <span className={cuNume ? '' : 'sr-only sm:not-sr-only'}>Autentificare</span>
      </button>
    );
  }

  const initiala = utilizator.nume.trim().charAt(0).toUpperCase() || '?';

  // focusul trece pe avatar inainte ca panoul sa dispara, ca modala sa-l poata reda la inchidere
  const deschide = (mod: ModAutentificare) => {
    buton.current?.focus();
    setDeschis(false);
    deschideModal(mod);
  };
  const iesire = () => {
    buton.current?.focus();
    setDeschis(false);
    deconecteaza().catch(() => undefined);
  };
  const laPierdereFocus = (e: FocusEvent<HTMLDivElement>) => {
    if (!radacina.current?.contains(e.relatedTarget as Node | null)) setDeschis(false);
  };

  return (
    <div ref={radacina} className="relative" onBlur={laPierdereFocus}>
      <button
        ref={buton}
        type="button"
        onClick={() => setDeschis((d) => !d)}
        aria-expanded={deschis}
        aria-controls={idPanou}
        aria-label={`Contul tău: ${utilizator.nume}`}
        className="flex items-center gap-2 rounded-full transition hover:opacity-90"
      >
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-[#7C3AED] text-sm font-bold text-white ring-2 ring-primary/20">
          {initiala}
        </span>
        {cuNume && (
          <span className="hidden max-w-[10rem] truncate text-sm font-semibold text-ink xl:block">{utilizator.nume}</span>
        )}
      </button>
      {deschis && (
        <div
          id={idPanou}
          className="absolute right-0 z-30 mt-2 w-64 rounded-2xl border border-line bg-card p-2 shadow-[0_12px_40px_rgb(0_0_0/0.35)]"
        >
          <div className="border-b border-line px-3 pb-2 pt-1">
            <p className="truncate text-sm font-semibold text-ink">{utilizator.nume}</p>
            <p className="truncate text-xs text-ink2">{utilizator.email}</p>
          </div>
          <ul className="pt-1">
            <li>
              <Link to="/profil" onClick={() => setDeschis(false)} className={CLASE_ELEMENT}>
                Profil
              </Link>
            </li>
            <li>
              <button type="button" onClick={() => deschide('schimbare-parola')} className={CLASE_ELEMENT}>
                Schimbă parola
              </button>
            </li>
            <li>
              <button type="button" onClick={iesire} className={`${CLASE_ELEMENT} text-red-500`}>
                Deconectare
              </button>
            </li>
          </ul>
        </div>
      )}
    </div>
  );
}
