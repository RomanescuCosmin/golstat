const numar = new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 });

function fmt(valoare: number | null, procent?: boolean): string {
  if (valoare == null) {
    return '—';
  }
  return procent ? `${Math.round(valoare)}%` : numar.format(valoare);
}

/** Latimea barei, proportional cu maximul randului; null → bara goala (nu 0 fals). */
function latime(valoare: number | null, celalalt: number | null): number {
  if (valoare == null || valoare <= 0) {
    return 0;
  }
  const maxim = Math.max(valoare, celalalt ?? 0);
  return Math.max((valoare / maxim) * 100, 8);
}

interface BaraComparativaProps {
  eticheta: string;
  gazde: number | null;
  oaspeti: number | null;
  /** Formateaza valorile ca procent (ex. posesie). */
  procent?: boolean;
}

/**
 * Un rand comparativ gazde (albastru) vs oaspeti (rosu) cu eticheta la centru si valorile
 * la capete; barele sunt proportionale cu maximul randului. Refolosit de "Statistici cheie"
 * (medii) si de "Statistici live" (valori dintr-un singur meci).
 */
export function BaraComparativa({ eticheta, gazde, oaspeti, procent }: BaraComparativaProps) {
  return (
    <div className="grid grid-cols-[2.75rem_1fr_auto_1fr_2.75rem] items-center gap-2 sm:gap-3">
      <span className="text-sm font-semibold tabular-nums text-ink">{fmt(gazde, procent)}</span>
      <div className="flex justify-end">
        <div className="h-[5px] rounded-full bg-primary" style={{ width: `${latime(gazde, oaspeti)}%` }} />
      </div>
      <span className="whitespace-nowrap px-1 text-center text-xs text-ink2 sm:text-sm">{eticheta}</span>
      <div>
        <div className="h-[5px] rounded-full bg-accent" style={{ width: `${latime(oaspeti, gazde)}%` }} />
      </div>
      <span className="text-right text-sm font-semibold tabular-nums text-ink">{fmt(oaspeti, procent)}</span>
    </div>
  );
}
