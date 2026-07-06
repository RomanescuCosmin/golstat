import { useEffect, useMemo, useRef } from 'react';
import { deruleazaLin } from '../../lib/derulare';
import { toISODataLocala } from '../../lib/format';
import { IconChevronLeft, IconChevronRight } from '../ui/icons';

/** Cate zile inainte si dupa azi sunt in banda. */
const INTERVAL_ZILE = 21;
/** Latimea fixa a unei celule de zi — folosita si la centrat, deci trebuie sa ramana sincronizata cu CSS-ul. */
const LATIME_ZI = 76;

const ziSaptamana = new Intl.DateTimeFormat('ro-RO', { weekday: 'short' });
const ziLuna = new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'short' });

function etichetaZi(d: Date): string {
  const brut = ziSaptamana.format(d).replace('.', '');
  return brut.charAt(0).toUpperCase() + brut.slice(1);
}

function plusZile(baza: Date, zile: number): Date {
  const d = new Date(baza);
  d.setDate(d.getDate() + zile);
  return d;
}

interface BandaZileProps {
  /** Ziua selectata, "YYYY-MM-DD". */
  selectata: string;
  onSelect: (dataISO: string) => void;
}

/**
 * Caruselul de zile (ca banda de date din mockup): azi ± 21 de zile intr-o banda derulanta
 * cu miscarea lina din caruselul live; ziua selectata e centrata si evidentiata cu gradient.
 */
export function BandaZile({ selectata, onSelect }: BandaZileProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const centratInstant = useRef(false);

  const aziISO = toISODataLocala(new Date());
  const zile = useMemo(() => {
    const azi = new Date();
    return Array.from({ length: INTERVAL_ZILE * 2 + 1 }, (_, i) => plusZile(azi, i - INTERVAL_ZILE));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aziISO]);

  // Ziua selectata sta in centrul benzii: instant la montare, lin la fiecare schimbare.
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const idx = zile.findIndex((z) => toISODataLocala(z) === selectata);
    if (idx < 0) return;
    const tinta = idx * LATIME_ZI - (el.clientWidth - LATIME_ZI) / 2;
    if (centratInstant.current) {
      deruleazaLin(el, tinta - el.scrollLeft);
    } else {
      centratInstant.current = true;
      el.scrollLeft = tinta;
    }
  }, [selectata, zile]);

  const sare = (directie: number) => {
    const el = scrollRef.current;
    if (el) deruleazaLin(el, directie * 4 * LATIME_ZI);
  };

  return (
    <div className="flex items-center gap-1 rounded-xl border border-line bg-card p-1.5 shadow-card dark:shadow-none">
      <button
        type="button"
        aria-label="Zile anterioare"
        onClick={() => sare(-1)}
        className="flex h-10 w-8 shrink-0 items-center justify-center rounded-lg text-ink2 transition hover:bg-bg hover:text-ink"
      >
        <IconChevronLeft width={16} height={16} />
      </button>

      <div
        ref={scrollRef}
        className="flex flex-1 overflow-x-auto [mask-image:linear-gradient(90deg,transparent,black_20px,black_calc(100%-20px),transparent)] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {zile.map((zi) => {
          const iso = toISODataLocala(zi);
          const activa = iso === selectata;
          const eAzi = iso === aziISO;
          return (
            <button
              key={iso}
              type="button"
              onClick={() => onSelect(iso)}
              aria-pressed={activa}
              className={`flex w-[76px] shrink-0 flex-col items-center rounded-lg px-1 py-1.5 leading-tight transition ${
                activa
                  ? 'bg-gradient-to-br from-primary to-[#7C3AED] text-white shadow-[0_4px_14px_rgb(var(--gs-primary)/0.35)]'
                  : 'text-ink2 hover:bg-bg hover:text-ink'
              }`}
            >
              <span
                className={`text-xs font-medium ${activa ? 'text-white/85' : eAzi ? 'font-semibold text-primary' : ''}`}
              >
                {eAzi ? 'Astăzi' : etichetaZi(zi)}
              </span>
              <span className="text-sm font-semibold">{ziLuna.format(zi).replace('.', '')}</span>
            </button>
          );
        })}
      </div>

      <button
        type="button"
        aria-label="Zile următoare"
        onClick={() => sare(1)}
        className="flex h-10 w-8 shrink-0 items-center justify-center rounded-lg text-ink2 transition hover:bg-bg hover:text-ink"
      >
        <IconChevronRight width={16} height={16} />
      </button>
    </div>
  );
}
