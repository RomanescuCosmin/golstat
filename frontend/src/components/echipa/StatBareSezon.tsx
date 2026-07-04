import type { StatBareSezon as StatBareSezonData } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

interface Bara {
  eticheta: string;
  valoare: number | null;
  /** Maximul fata de care scalam latimea barei. */
  max: number;
  procent?: boolean;
}

function formatVal(valoare: number, procent?: boolean): string {
  return procent ? `${Math.round(valoare)}%` : new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 }).format(valoare);
}

function RandBara({ eticheta, valoare, max, procent }: Bara) {
  const latime = valoare != null && valoare > 0 ? Math.min((valoare / max) * 100, 100) : 0;
  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <span className="text-sm text-ink2">{eticheta}</span>
        <span className="text-sm font-semibold tabular-nums text-ink">
          {valoare == null ? '—' : formatVal(valoare, procent)}
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-ink2/10 dark:bg-ink2/15">
        <div className="h-full rounded-full bg-primary" style={{ width: `${latime}%` }} />
      </div>
    </div>
  );
}

interface StatBareSezonProps {
  statistici: StatBareSezonData | null;
}

/** "Statistici sezon": bare orizontale mono-culoare pentru mediile echipei + meciuri fara gol primit. */
export function StatBareSezon({ statistici }: StatBareSezonProps) {
  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Statistici sezon</h2>
      {!statistici ? (
        <EmptyState titlu="Fără statistici" mesaj="Nu există date de sezon pentru această echipă." />
      ) : (
        <div className="mt-4 space-y-3.5">
          <RandBara eticheta="Goluri marcate / meci" valoare={statistici.goluriMarcatePeMeci} max={3.5} />
          <RandBara eticheta="Goluri primite / meci" valoare={statistici.goluriPrimitePeMeci} max={3.5} />
          <RandBara eticheta="Șuturi pe meci" valoare={statistici.suturiPeMeci} max={25} />
          <RandBara eticheta="Posesie medie" valoare={statistici.posesieMedie} max={100} procent />
          <RandBara eticheta="Precizie pase" valoare={statistici.preciziePase} max={100} procent />
          <div className="flex items-center justify-between border-t border-line pt-3">
            <span className="text-sm text-ink2">Meciuri fără gol primit</span>
            <span className="text-lg font-extrabold tabular-nums text-ink">{statistici.cleanSheets ?? '—'}</span>
          </div>
        </div>
      )}
    </Card>
  );
}
