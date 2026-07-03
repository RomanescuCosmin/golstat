import { useState } from 'react';
import { toISODataLocala } from '../../lib/format';
import { IconChevronLeft, IconChevronRight } from '../ui/icons';

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

/** Banda de navigare pe zile (Vin/Sâm/Dum…), cu sageti stanga/dreapta; ziua de azi e punctul de plecare. */
export function BandaZile({ selectata, onSelect }: BandaZileProps) {
  // Fereastra de 4 zile; offset fata de azi (implicit incepe cu ziua de ieri, ca in design).
  const [start, setStart] = useState(-1);
  const azi = new Date();
  const zile = [0, 1, 2, 3].map((i) => plusZile(azi, start + i));

  return (
    <div className="flex items-center gap-1 rounded-xl border border-line bg-card p-1.5 shadow-card dark:shadow-none">
      <button
        type="button"
        aria-label="Zile anterioare"
        onClick={() => setStart((s) => s - 1)}
        className="flex h-9 w-8 shrink-0 items-center justify-center rounded-lg text-ink2 transition hover:bg-bg hover:text-ink"
      >
        <IconChevronLeft width={16} height={16} />
      </button>

      {zile.map((zi) => {
        const iso = toISODataLocala(zi);
        const activa = iso === selectata;
        return (
          <button
            key={iso}
            type="button"
            onClick={() => onSelect(iso)}
            aria-pressed={activa}
            className={`flex min-w-[4.5rem] flex-col items-center rounded-lg px-3 py-1.5 leading-tight transition ${
              activa ? 'bg-primary text-white' : 'text-ink2 hover:bg-bg hover:text-ink'
            }`}
          >
            <span className={`text-xs font-medium ${activa ? 'text-white/80' : ''}`}>{etichetaZi(zi)}</span>
            <span className="text-sm font-semibold">{ziLuna.format(zi).replace('.', '')}</span>
          </button>
        );
      })}

      <button
        type="button"
        aria-label="Zile următoare"
        onClick={() => setStart((s) => s + 1)}
        className="flex h-9 w-8 shrink-0 items-center justify-center rounded-lg text-ink2 transition hover:bg-bg hover:text-ink"
      >
        <IconChevronRight width={16} height={16} />
      </button>
    </div>
  );
}
