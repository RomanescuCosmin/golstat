import { useEffect, useId, useRef, useState, type ReactNode } from 'react';

export const CLASE_CHIP =
  'inline-flex h-10 items-center gap-2 rounded-full border px-4 text-sm font-semibold transition focus:outline-none focus-visible:ring-2 focus-visible:ring-piata/40';

export function claseChip(activ: boolean): string {
  return `${CLASE_CHIP} ${
    activ
      ? 'border-piata/60 bg-piata/15 text-ink hover:bg-piata/20'
      : 'border-line bg-card text-ink hover:border-ink2/40'
  }`;
}

export function Sageata({ deschis = false }: { deschis?: boolean }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      width={14}
      height={14}
      fill="none"
      stroke="currentColor"
      strokeWidth={2.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`shrink-0 text-ink2 transition ${deschis ? 'rotate-180' : ''}`}
    >
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

interface Props {
  /** Numele accesibil al butonului (ex. „Tip piață"). */
  titlu: string;
  /** Ce se vede pe chip: valoarea curentă. */
  eticheta: ReactNode;
  icona?: ReactNode;
  /** Chip evidențiat = filtrul nu e pe valoarea implicită. */
  activ?: boolean;
  /** Conținutul meniului; primește `inchide` ca alegerile simple să închidă meniul. */
  children: (inchide: () => void) => ReactNode;
  latime?: string;
}

/**
 * Un filtru compact: chip-ul arată alegerea curentă, iar click-ul deschide un meniu mic sub el.
 * Se închide la click în afară, la Escape sau când conținutul cere (`inchide`).
 */
export function ChipMeniu({ titlu, eticheta, icona, activ = false, children, latime = 'w-72' }: Props) {
  const [deschis, setDeschis] = useState(false);
  const radacina = useRef<HTMLDivElement>(null);
  const id = useId();

  useEffect(() => {
    if (!deschis) return;
    const laClick = (e: MouseEvent) => {
      if (!radacina.current?.contains(e.target as Node)) setDeschis(false);
    };
    const laTasta = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setDeschis(false);
    };
    document.addEventListener('mousedown', laClick);
    document.addEventListener('keydown', laTasta);
    return () => {
      document.removeEventListener('mousedown', laClick);
      document.removeEventListener('keydown', laTasta);
    };
  }, [deschis]);

  return (
    <div ref={radacina} className="relative">
      <button
        type="button"
        aria-label={titlu}
        aria-expanded={deschis}
        aria-controls={id}
        onClick={() => setDeschis((d) => !d)}
        className={claseChip(activ || deschis)}
      >
        {icona && <span aria-hidden="true">{icona}</span>}
        <span className="max-w-[14rem] truncate">{eticheta}</span>
        <Sageata deschis={deschis} />
      </button>
      {deschis && (
        <div
          id={id}
          role="dialog"
          aria-label={titlu}
          className={`absolute left-0 top-full z-30 mt-2 max-w-[calc(100vw-2rem)] rounded-2xl border border-line bg-card p-3 shadow-[0_12px_40px_rgb(0_0_0/0.35)] ${latime}`}
        >
          {children(() => setDeschis(false))}
        </div>
      )}
    </div>
  );
}
