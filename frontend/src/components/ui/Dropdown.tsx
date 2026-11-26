import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { IconCheck, IconChevronDown } from './icons';

export interface OptiuneDropdown<T> {
  valoare: T;
  eticheta: string;
  icon?: ReactNode;
  /** Text mic sub eticheta (ex. tara ligii). */
  detaliu?: string;
}

interface DropdownProps<T> {
  /** Numele accesibil (aria-label) al controlului. */
  eticheta: string;
  valoare: T;
  optiuni: OptiuneDropdown<T>[];
  onSchimba: (valoare: T) => void;
  /** Iconita fixa din stanga butonului, cand optiunea aleasa nu are una proprie. */
  icon?: ReactNode;
  /** `pastila` = rotund, compact (sezon, grupa); `camp` = camp inalt (selector de competitie). */
  varianta?: 'pastila' | 'camp';
  /** Meniul se aliniaza la dreapta butonului (controale lipite de marginea dreapta). */
  dreapta?: boolean;
  className?: string;
}

/**
 * Dropdown-ul aplicatiei, in locul `<select>`-ului nativ (lista nativa nu se poate stiliza): buton
 * `combobox` + `listbox` pe culorile temei, cu navigare din tastatura (sageti, Home/End, Enter, Escape).
 */
export function Dropdown<T extends string | number>({
  eticheta,
  valoare,
  optiuni,
  onSchimba,
  icon,
  varianta = 'pastila',
  dreapta = false,
  className = '',
}: DropdownProps<T>) {
  const [deschis, setDeschis] = useState(false);
  const [activ, setActiv] = useState(0);
  const radacina = useRef<HTMLDivElement>(null);
  const lista = useRef<HTMLUListElement>(null);
  const id = useId();

  const indexAles = Math.max(
    0,
    optiuni.findIndex((o) => o.valoare === valoare),
  );
  const aleasa = optiuni[indexAles];

  useEffect(() => {
    if (!deschis) return;
    const laClick = (e: MouseEvent) => {
      if (!radacina.current?.contains(e.target as Node)) setDeschis(false);
    };
    document.addEventListener('mousedown', laClick);
    return () => document.removeEventListener('mousedown', laClick);
  }, [deschis]);

  useEffect(() => {
    if (!deschis) return;
    lista.current?.querySelector<HTMLElement>(`[data-index="${activ}"]`)?.scrollIntoView?.({ block: 'nearest' });
  }, [deschis, activ]);

  function deschide() {
    setActiv(indexAles);
    setDeschis(true);
  }

  function alege(i: number) {
    const o = optiuni[i];
    if (o) onSchimba(o.valoare);
    setDeschis(false);
  }

  function onKeyDown(e: KeyboardEvent) {
    if (!deschis) {
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) {
        e.preventDefault();
        deschide();
      }
      return;
    }
    const n = optiuni.length;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiv((i) => (i + 1) % n);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiv((i) => (i - 1 + n) % n);
    } else if (e.key === 'Home') {
      e.preventDefault();
      setActiv(0);
    } else if (e.key === 'End') {
      e.preventDefault();
      setActiv(n - 1);
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      alege(activ);
    } else if (e.key === 'Escape' || e.key === 'Tab') {
      setDeschis(false);
    }
  }

  const iconButon = aleasa?.icon ?? icon;
  const claseButon =
    varianta === 'camp'
      ? 'h-11 w-full rounded-input pl-3 pr-3 text-sm font-medium shadow-card dark:shadow-none'
      : 'h-10 rounded-full pl-4 pr-3 text-sm font-semibold';

  return (
    <div ref={radacina} className={`relative ${varianta === 'camp' ? 'w-full sm:w-auto sm:min-w-[15rem]' : ''} ${className}`}>
      <button
        type="button"
        role="combobox"
        aria-label={eticheta}
        aria-haspopup="listbox"
        aria-expanded={deschis}
        aria-controls={id}
        aria-activedescendant={deschis ? `${id}-${activ}` : undefined}
        onClick={() => (deschis ? setDeschis(false) : deschide())}
        onKeyDown={onKeyDown}
        className={`flex cursor-pointer items-center gap-2 border bg-card text-left text-ink transition focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${claseButon} ${
          deschis ? 'border-primary/60 ring-2 ring-primary/20' : 'border-line hover:border-ink2/40'
        }`}
      >
        {iconButon && (
          <span aria-hidden className="flex shrink-0 items-center text-ink2">
            {iconButon}
          </span>
        )}
        <span className="min-w-0 flex-1 truncate">{aleasa?.eticheta ?? ''}</span>
        <IconChevronDown
          width={16}
          height={16}
          className={`shrink-0 text-ink2 transition-transform duration-200 ${deschis ? 'rotate-180 text-primary' : ''}`}
        />
      </button>

      {deschis && (
        <ul
          ref={lista}
          id={id}
          role="listbox"
          aria-label={eticheta}
          className={`absolute top-full z-40 mt-2 max-h-80 min-w-full max-w-[calc(100vw-1.5rem)] animate-fade-in overflow-y-auto overscroll-contain rounded-2xl border border-line bg-card p-1.5 shadow-[0_16px_48px_rgb(0_0_0/0.4)] ${
            dreapta ? 'right-0' : 'left-0'
          } ${varianta === 'camp' ? 'w-full sm:w-72' : 'w-max'}`}
        >
          {optiuni.map((o, i) => {
            const ales = i === indexAles;
            return (
              <li
                key={String(o.valoare)}
                id={`${id}-${i}`}
                data-index={i}
                role="option"
                aria-selected={ales}
                onMouseEnter={() => setActiv(i)}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => alege(i)}
                className={`flex min-h-[42px] cursor-pointer items-center gap-2.5 rounded-xl px-3 py-2 text-sm transition ${
                  i === activ ? 'bg-ink2/10' : ''
                } ${ales ? 'font-semibold text-primary' : 'text-ink'}`}
              >
                {o.icon && (
                  <span aria-hidden className="flex w-5 shrink-0 justify-center">
                    {o.icon}
                  </span>
                )}
                <span className="min-w-0 flex-1">
                  <span className="block truncate">{o.eticheta}</span>
                  {o.detaliu && <span className="block truncate text-xs font-normal text-ink2">{o.detaliu}</span>}
                </span>
                {ales && <IconCheck width={16} height={16} className="shrink-0 text-primary" />}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
