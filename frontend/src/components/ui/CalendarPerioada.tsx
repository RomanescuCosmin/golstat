import { useEffect, useRef, useState } from 'react';

/** Perioadă cu date ISO „YYYY-MM-DD", inclusiv capetele. */
export interface PerioadaCalendar {
  de: string;
  pana: string;
}

const LUNI = ['ianuarie', 'februarie', 'martie', 'aprilie', 'mai', 'iunie', 'iulie', 'august', 'septembrie',
  'octombrie', 'noiembrie', 'decembrie'];
const ZILE_SAPTAMANA = ['L', 'Ma', 'Mi', 'J', 'V', 'S', 'D'];

function iso(d: Date): string {
  const z = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}

function dinIso(s: string): Date {
  const [a, l, z] = s.split('-').map(Number);
  return new Date(a, l - 1, z);
}

function plusZile(s: string, zile: number): string {
  const d = dinIso(s);
  d.setDate(d.getDate() + zile);
  return iso(d);
}

function scurt(s: string): string {
  const d = dinIso(s);
  return `${d.getDate()} ${LUNI[d.getMonth()].slice(0, 3)}.`;
}

/**
 * Selector de perioadă cu calendar: un buton cu perioada aleasă deschide luna; primul clic fixează
 * începutul, al doilea sfârșitul (zilele dintre ele se colorează). Zilele trecute și cele peste
 * {@code maxZile} de la început nu se pot alege.
 */
export function CalendarPerioada({
  valoare,
  onSchimba,
  maxZile = 14,
}: {
  valoare: PerioadaCalendar;
  onSchimba: (p: PerioadaCalendar) => void;
  maxZile?: number;
}) {
  const azi = iso(new Date());
  const [deschis, setDeschis] = useState(false);
  const [inceput, setInceput] = useState<string | null>(null);
  const [luna, setLuna] = useState(() => {
    const d = dinIso(valoare.de);
    return new Date(d.getFullYear(), d.getMonth(), 1);
  });
  const radacina = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!deschis) return;
    const inchide = (e: MouseEvent) => {
      if (radacina.current && !radacina.current.contains(e.target as Node)) {
        setDeschis(false);
        setInceput(null);
      }
    };
    document.addEventListener('mousedown', inchide);
    return () => document.removeEventListener('mousedown', inchide);
  }, [deschis]);

  const prima = new Date(luna.getFullYear(), luna.getMonth(), 1);
  const decalaj = (prima.getDay() + 6) % 7;
  const zileInLuna = new Date(luna.getFullYear(), luna.getMonth() + 1, 0).getDate();
  const celule: (string | null)[] = [
    ...Array.from({ length: decalaj }, () => null),
    ...Array.from({ length: zileInLuna }, (_, i) => iso(new Date(luna.getFullYear(), luna.getMonth(), i + 1))),
  ];

  const de = inceput ?? valoare.de;
  const pana = inceput ? inceput : valoare.pana;

  function alege(zi: string) {
    if (!inceput) {
      setInceput(zi);
      return;
    }
    const [a, b] = zi < inceput ? [zi, inceput] : [inceput, zi];
    onSchimba({ de: a, pana: b });
    setInceput(null);
    setDeschis(false);
  }

  function dezactivata(zi: string): boolean {
    if (zi < azi) return true;
    return inceput != null && (zi > plusZile(inceput, maxZile - 1) || zi < plusZile(inceput, -(maxZile - 1)));
  }

  return (
    <div className="relative" ref={radacina}>
      <button
        type="button"
        onClick={() => setDeschis((d) => !d)}
        aria-expanded={deschis}
        aria-label="Alege perioada"
        className="flex items-center gap-2 rounded-lg border border-line bg-card px-3 py-1.5 text-sm font-semibold text-ink transition hover:border-primary/50"
      >
        <span aria-hidden>📅</span>
        {valoare.de === valoare.pana ? scurt(valoare.de) : `${scurt(valoare.de)} – ${scurt(valoare.pana)}`}
      </button>
      {deschis && (
        <div
          role="dialog"
          aria-label="Calendar"
          className="absolute left-0 z-30 mt-2 w-72 rounded-card border border-line bg-card p-3 shadow-cardHover"
        >
          <div className="mb-2 flex items-center justify-between">
            <button
              type="button"
              aria-label="Luna anterioară"
              onClick={() => setLuna(new Date(luna.getFullYear(), luna.getMonth() - 1, 1))}
              className="rounded-md px-2 py-1 text-ink2 hover:bg-ink2/10 hover:text-ink"
            >
              ‹
            </button>
            <span className="text-sm font-bold capitalize text-ink">
              {LUNI[luna.getMonth()]} {luna.getFullYear()}
            </span>
            <button
              type="button"
              aria-label="Luna următoare"
              onClick={() => setLuna(new Date(luna.getFullYear(), luna.getMonth() + 1, 1))}
              className="rounded-md px-2 py-1 text-ink2 hover:bg-ink2/10 hover:text-ink"
            >
              ›
            </button>
          </div>
          <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-semibold text-ink2">
            {ZILE_SAPTAMANA.map((z) => (
              <span key={z}>{z}</span>
            ))}
          </div>
          <div className="mt-1 grid grid-cols-7 gap-1">
            {celule.map((zi, i) => {
              if (!zi) return <span key={`gol-${i}`} />;
              const capat = zi === de || zi === pana;
              const inInterval = zi > de && zi < pana;
              const blocata = dezactivata(zi);
              return (
                <button
                  key={zi}
                  type="button"
                  disabled={blocata}
                  onClick={() => alege(zi)}
                  aria-label={zi}
                  aria-pressed={capat}
                  className={`h-8 rounded-md text-sm tabular-nums transition ${
                    capat
                      ? 'bg-primary font-bold text-white'
                      : inInterval
                        ? 'bg-primary/15 text-ink'
                        : blocata
                          ? 'cursor-not-allowed text-ink2/40'
                          : 'text-ink hover:bg-ink2/10'
                  } ${zi === azi && !capat ? 'ring-1 ring-primary/50' : ''}`}
                >
                  {dinIso(zi).getDate()}
                </button>
              );
            })}
          </div>
          <p className="mt-2 text-[11px] text-ink2">
            {inceput ? 'Alege ziua de sfârșit.' : `Alege ziua de început (maxim ${maxZile} zile).`}
          </p>
        </div>
      )}
    </div>
  );
}
