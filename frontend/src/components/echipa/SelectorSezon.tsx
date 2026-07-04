import { IconChevronDown } from '../ui/icons';

interface SelectorSezonProps {
  sezoane: number[];
  valoare: number | null;
  onChange: (sezon: number) => void;
}

/** Formatul "2023/24" dintr-un an de start de sezon. */
function etichetaSezon(an: number): string {
  const urm = String((an + 1) % 100).padStart(2, '0');
  return `${an}/${urm}`;
}

/** Dropdown de sezon pentru pagina echipei; re-fetch la schimbare. */
export function SelectorSezon({ sezoane, valoare, onChange }: SelectorSezonProps) {
  if (sezoane.length === 0) {
    return null;
  }
  return (
    <div className="relative">
      <select
        value={valoare ?? sezoane[0]}
        onChange={(e) => onChange(Number(e.target.value))}
        aria-label="Sezon"
        className="h-9 appearance-none rounded-full border border-line bg-card pl-4 pr-9 text-sm font-semibold text-ink focus:outline-none focus:ring-2 focus:ring-primary/40"
      >
        {sezoane.map((an) => (
          <option key={an} value={an}>
            {etichetaSezon(an)}
          </option>
        ))}
      </select>
      <IconChevronDown className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-ink2" width={16} height={16} />
    </div>
  );
}
