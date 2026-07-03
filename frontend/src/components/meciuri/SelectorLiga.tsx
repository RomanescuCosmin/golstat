import { LIGI } from '../../lib/ligi';
import { IconChevronDown, IconGlobe } from '../ui/icons';

interface SelectorLigaProps {
  leagueId: number;
  onChange: (leagueId: number) => void;
}

/** Dropdown de competitii, stilul pastilei "Toate competițiile" din design. */
export function SelectorLiga({ leagueId, onChange }: SelectorLigaProps) {
  return (
    <label className="relative flex items-center">
      <span className="sr-only">Competiție</span>
      <IconGlobe
        width={16}
        height={16}
        className="pointer-events-none absolute left-3 text-ink2"
      />
      <select
        value={leagueId}
        onChange={(e) => onChange(Number(e.target.value))}
        className="h-11 w-full appearance-none rounded-xl border border-line bg-card pl-9 pr-9 text-sm font-medium text-ink shadow-card focus:outline-none focus:ring-2 focus:ring-primary/40 dark:shadow-none"
      >
        {LIGI.map((liga) => (
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
