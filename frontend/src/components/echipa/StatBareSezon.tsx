import type { StatBareSezon as StatBareSezonData, SumarSezon } from '../../api/types';
import { useGrowOnMount } from '../../hooks/useAnimatii';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

const nrScurt = new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 });
const nrPeMeci = new Intl.NumberFormat('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

interface RandBaraProps {
  eticheta: string;
  valoare: number | null;
  /** Maximul fata de care scalam latimea barei. */
  max: number;
  procent?: boolean;
  /** Bara rosie (ex. goluri primite) in loc de albastru. */
  accent?: boolean;
  /** Sublabel discret la dreapta valorii (ex. "2,00 pe meci"). */
  subEticheta?: string | null;
}

function RandBara({ eticheta, valoare, max, procent, accent, subEticheta }: RandBaraProps) {
  const montat = useGrowOnMount();
  const latime = valoare != null && valoare > 0 && max > 0 ? Math.min((valoare / max) * 100, 100) : 0;
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between gap-2">
        <span className="text-sm text-ink2">{eticheta}</span>
        <span className="flex items-baseline gap-1.5">
          <span className="text-sm font-bold tabular-nums text-ink">
            {valoare == null ? '—' : procent ? `${Math.round(valoare)}%` : nrScurt.format(valoare)}
          </span>
          {subEticheta && <span className="text-[11px] tabular-nums text-ink2/70">{subEticheta}</span>}
        </span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-bg dark:bg-ink2/15">
        <div
          className={`h-full rounded-full transition-[width] duration-500 ease-out motion-reduce:transition-none ${accent ? 'bg-accent' : 'bg-primary'}`}
          style={{ width: montat ? `${latime}%` : '0%' }}
        />
      </div>
    </div>
  );
}

interface StatBareSezonProps {
  statistici: StatBareSezonData | null;
  sumar: SumarSezon | null;
}

/** "Statistici sezon": totaluri de goluri + bare orizontale (albastru; roșu la goluri primite). */
export function StatBareSezon({ statistici, sumar }: StatBareSezonProps) {
  if (!statistici && !sumar) {
    return (
      <Card className="p-5">
        <h2 className="text-sm font-extrabold text-ink">Statistici sezon</h2>
        <EmptyState titlu="Fără statistici" mesaj="Nu există date de sezon pentru această echipă." />
      </Card>
    );
  }

  const marcate = sumar?.goluriMarcate ?? null;
  const primite = sumar?.goluriPrimite ?? null;
  // Scalam ambele bare de goluri la acelasi maxim, ca sa fie comparabile vizual.
  const maxGoluri = Math.max(marcate ?? 0, primite ?? 0);

  return (
    <Card className="p-5">
      <h2 className="text-sm font-extrabold text-ink">Statistici sezon</h2>
      <div className="mt-4 space-y-4">
        <RandBara
          eticheta="Goluri marcate"
          valoare={marcate}
          max={maxGoluri}
          subEticheta={statistici?.goluriMarcatePeMeci != null ? `${nrPeMeci.format(statistici.goluriMarcatePeMeci)} pe meci` : null}
        />
        <RandBara
          eticheta="Goluri primite"
          valoare={primite}
          max={maxGoluri}
          accent
          subEticheta={statistici?.goluriPrimitePeMeci != null ? `${nrPeMeci.format(statistici.goluriPrimitePeMeci)} pe meci` : null}
        />
        <RandBara eticheta="Șuturi pe meci" valoare={statistici?.suturiPeMeci ?? null} max={25} />
        <RandBara eticheta="Posesie medie" valoare={statistici?.posesieMedie ?? null} max={100} procent />
        <RandBara eticheta="Pase reușite" valoare={statistici?.preciziePase ?? null} max={100} procent />
        <div className="flex items-center justify-between border-t border-line pt-3.5">
          <span className="text-sm text-ink2">Meciuri fără gol primit</span>
          <span className="text-lg font-extrabold tabular-nums text-primary">{statistici?.cleanSheets ?? '—'}</span>
        </div>
      </div>
    </Card>
  );
}
