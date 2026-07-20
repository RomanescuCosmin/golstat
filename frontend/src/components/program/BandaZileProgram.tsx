import type { ProgramZi } from '../../api/types';
import { toISODataLocala } from '../../lib/format';

const ziSaptamana = new Intl.DateTimeFormat('ro-RO', { weekday: 'short' });
const ziLuna = new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'short' });

// "YYYY-MM-DD" e parsat de `new Date` ca miezul noptii UTC — construim pe componente, in fusul local.
function dataDinISO(iso: string): Date {
  const [an, luna, zi] = iso.split('-').map(Number);
  return new Date(an, luna - 1, zi);
}

function etichetaZi(iso: string, aziISO: string): string {
  if (iso === aziISO) {
    return 'Astăzi';
  }
  const brut = ziSaptamana.format(dataDinISO(iso)).replace('.', '');
  return brut.charAt(0).toUpperCase() + brut.slice(1);
}

function numarMeciuri(zi: ProgramZi): number {
  return zi.ligi.reduce((n, l) => n + l.meciuri.length, 0);
}

interface BandaZileProgramProps {
  /** Zilele venite din API (doar cele cu meciuri). */
  zile: ProgramZi[];
  /** Ziua selectata, "YYYY-MM-DD"; `null` = toate zilele. */
  selectata: string | null;
  onSelect: (dataISO: string | null) => void;
}

/** Chips-uri pentru zilele din program: „Toate zilele" + cate una per zi, cu numarul de meciuri. */
export function BandaZileProgram({ zile, selectata, onSelect }: BandaZileProgramProps) {
  const aziISO = toISODataLocala(new Date());

  return (
    <div className="flex flex-wrap items-stretch gap-2 rounded-xl border border-line bg-card p-1.5 shadow-card dark:shadow-none">
      <Chip activ={selectata == null} onClick={() => onSelect(null)}>
        <span className="text-sm font-semibold">Toate zilele</span>
      </Chip>

      {zile.map((zi) => {
        const activ = zi.data === selectata;
        const total = numarMeciuri(zi);
        return (
          <Chip key={zi.data} activ={activ} onClick={() => onSelect(zi.data)}>
            <span className={`text-xs font-medium ${activ ? 'text-white/85' : zi.data === aziISO ? 'font-semibold text-primary' : ''}`}>
              {etichetaZi(zi.data, aziISO)}
            </span>
            <span className="text-sm font-semibold">{ziLuna.format(dataDinISO(zi.data)).replace('.', '')}</span>
            <span className={`text-[11px] ${activ ? 'text-white/75' : 'text-ink2'}`}>
              {total} {total === 1 ? 'meci' : 'meciuri'}
            </span>
          </Chip>
        );
      })}
    </div>
  );
}

function Chip({ activ, onClick, children }: { activ: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activ}
      className={`flex min-w-[84px] flex-col items-center justify-center rounded-lg px-3 py-1.5 leading-tight transition ${
        activ
          ? 'bg-gradient-to-br from-primary to-[#7C3AED] text-white shadow-[0_4px_14px_rgb(var(--gs-primary)/0.35)]'
          : 'text-ink2 hover:bg-bg hover:text-ink'
      }`}
    >
      {children}
    </button>
  );
}
