import { IconChevronDown, IconGlobe } from '../ui/icons';

/** O optiune de competitie in selector. */
export interface OptiuneLiga {
  id: number;
  nume: string;
}

interface SelectorLigaProps {
  /** Liga selectata; `null` = toate competitiile. */
  leagueId: number | null;
  onChange: (leagueId: number | null) => void;
  /** Competitiile disponibile (dinamic, din ziua incarcata). Prima optiune e mereu „Toate competițiile". */
  optiuni: OptiuneLiga[];
}

/** Dropdown de competitii; optiunile vin dinamic din ligile zilei, deci reflecta exact ce e afisat. */
export function SelectorLiga({ leagueId, onChange, optiuni }: SelectorLigaProps) {
  return (
    <label className="relative flex items-center">
      <span className="sr-only">Competiție</span>
      <IconGlobe
        width={16}
        height={16}
        className="pointer-events-none absolute left-3 text-ink2"
      />
      <select
        value={leagueId ?? ''}
        onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
        className="h-11 w-full appearance-none rounded-xl border border-line bg-card pl-9 pr-9 text-sm font-medium text-ink shadow-card focus:outline-none focus:ring-2 focus:ring-primary/40 dark:shadow-none"
      >
        <option value="">Toate competițiile</option>
        {optiuni.map((liga) => (
          <option key={liga.id} value={liga.id}>
            {liga.nume}
          </option>
        ))}
      </select>
      <IconChevronDown
        width={14}
        height={14}
        className="pointer-events-none absolute right-3 text-ink2"
      />
    </label>
  );
}
