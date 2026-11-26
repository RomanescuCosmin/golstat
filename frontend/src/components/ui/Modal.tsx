import { useEffect, useId, useRef, type MouseEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { IconX } from './icons';

interface ModalProps {
  deschis: boolean;
  onInchide: () => void;
  titlu: string;
  children: ReactNode;
  /** Cand se schimba (ex. alt pas al formularului), focusul revine pe primul camp. */
  cheieContinut?: string;
}

const FOCUSABILE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Fereastra modala accesibila: portal in `body`, restul paginii `inert`, focus tinut inauntru,
 * Escape / click pe fundal inchid, scroll-ul paginii blocat, focusul revine la elementul care a deschis-o.
 */
export function Modal({ deschis, onInchide, titlu, children, cheieContinut }: ModalProps) {
  const idTitlu = useId();
  const panou = useRef<HTMLDivElement>(null);
  const apasatPeFundal = useRef(false);
  const onInchideRef = useRef(onInchide);
  onInchideRef.current = onInchide;

  useEffect(() => {
    if (!deschis) return;
    const anterior = document.activeElement as HTMLElement | null;
    const radacina = document.getElementById('root');
    const overflow = document.body.style.overflow;
    radacina?.setAttribute('inert', '');
    document.body.style.overflow = 'hidden';

    const laTasta = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onInchideRef.current();
      } else if (e.key === 'Tab' && panou.current) {
        const elemente = Array.from(panou.current.querySelectorAll<HTMLElement>(FOCUSABILE));
        if (elemente.length === 0) return;
        const primul = elemente[0];
        const ultimul = elemente[elemente.length - 1];
        if (e.shiftKey && document.activeElement === primul) {
          e.preventDefault();
          ultimul.focus();
        } else if (!e.shiftKey && document.activeElement === ultimul) {
          e.preventDefault();
          primul.focus();
        }
      }
    };
    document.addEventListener('keydown', laTasta);
    return () => {
      document.removeEventListener('keydown', laTasta);
      radacina?.removeAttribute('inert');
      document.body.style.overflow = overflow;
      if (anterior?.isConnected) anterior.focus();
    };
  }, [deschis]);

  useEffect(() => {
    if (!deschis) return;
    (panou.current?.querySelector<HTMLElement>('input, select, textarea') ?? panou.current)?.focus();
  }, [deschis, cheieContinut]);

  // cand un pas al formularului inlocuieste elementul cu focus (ex. ecranul de succes), focusul ramane in fereastra
  useEffect(() => {
    const p = panou.current;
    if (!deschis || !p) return;
    const observator = new MutationObserver(() => {
      if (!p.contains(document.activeElement)) {
        (p.querySelector<HTMLElement>('input, select, textarea') ?? p).focus();
      }
    });
    observator.observe(p, { childList: true, subtree: true });
    return () => observator.disconnect();
  }, [deschis]);

  if (!deschis) return null;

  // se inchide doar daca si apasarea a inceput pe fundal (nu la selectarea textului dintr-un camp)
  const laMouseDown = (e: MouseEvent) => {
    apasatPeFundal.current = e.target === e.currentTarget;
  };
  const laClick = (e: MouseEvent) => {
    if (apasatPeFundal.current && e.target === e.currentTarget) onInchide();
    apasatPeFundal.current = false;
  };

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
      onMouseDown={laMouseDown}
      onClick={laClick}
      data-testid="modal-fundal"
    >
      <div
        ref={panou}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitlu}
        tabIndex={-1}
        className="relative max-h-[90vh] w-full max-w-md overflow-y-auto rounded-2xl border border-line bg-card p-6 shadow-[0_12px_40px_rgb(0_0_0/0.35)] outline-none"
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <h2 id={idTitlu} className="text-lg font-bold text-ink">
            {titlu}
          </h2>
          <button
            type="button"
            onClick={onInchide}
            aria-label="Închide"
            className="-m-2 rounded-full p-2 text-ink2 transition hover:bg-ink2/10 hover:text-ink"
          >
            <IconX width={18} height={18} />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  );
}
