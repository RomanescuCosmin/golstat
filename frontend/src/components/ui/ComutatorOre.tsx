import { FERESTRE_ORE } from '../../lib/piete';
import { IconCeas } from './icons';

/** Fereastra orara: „Oricând" sau doar meciurile care incep in urmatoarele 3 / 5 / 8 ore. */
export function ComutatorOre({ valoare, onSchimba }: { valoare: number | null; onSchimba: (ore: number | null) => void }) {
  const optiune = (v: number | null, eticheta: string) => (
    <button
      key={eticheta}
      type="button"
      onClick={() => onSchimba(v)}
      aria-pressed={valoare === v}
      className={`whitespace-nowrap rounded-md px-3 py-1 text-xs font-semibold transition ${
        valoare === v ? 'bg-primary text-white' : 'text-ink2 hover:text-ink'
      }`}
    >
      {eticheta}
    </button>
  );
  return (
    <div className="inline-flex items-center rounded-lg border border-line p-0.5" role="group" aria-label="Interval orar">
      <IconCeas width={14} height={14} className="mx-1.5 shrink-0 text-ink2" />
      {optiune(null, 'Oricând')}
      {FERESTRE_ORE.map((ore) => optiune(ore, `În ${ore} ore`))}
    </div>
  );
}
