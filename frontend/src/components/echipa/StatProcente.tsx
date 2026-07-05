import type { StatProcent } from '../../api/types';
import { useGrowOnMount } from '../../hooks/useAnimatii';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

const ETICHETE: Record<StatProcent['categorie'], string> = {
  GOLURI: 'Goluri',
  CORNERE: 'Cornere',
  FAULTURI: 'Faulturi',
  CARTONASE: 'Cartonașe',
};

function formatNumar(v: number | null): string {
  return v == null ? '—' : new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 }).format(v);
}

/** Bară per categorie relativă la media ligii: 50% = media ligii (marker), plus valoarea echipei și a ligii. */
export function StatProcente({ statProcente }: { statProcente: StatProcent[] }) {
  const montat = useGrowOnMount();
  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Statistici vs. media ligii</h2>
      {statProcente.length === 0 ? (
        <EmptyState titlu="Fără date" mesaj="Nu există statistici comparabile pentru acest sezon." />
      ) : (
        <div className="mt-4 space-y-4">
          {statProcente.map((s) => (
            <div key={s.categorie}>
              <div className="mb-1 flex items-baseline justify-between">
                <span className="text-sm font-semibold text-ink">{ETICHETE[s.categorie]}</span>
                <span className="text-sm font-bold tabular-nums text-ink">
                  {formatNumar(s.medieEchipa)} <span className="text-xs font-medium text-ink2">/ meci</span>
                </span>
              </div>
              <div className="relative h-2.5 overflow-hidden rounded-full bg-ink2/10 dark:bg-ink2/15">
                <div
                  className={`h-full rounded-full transition-[width] duration-500 ease-out motion-reduce:transition-none ${s.procent >= 50 ? 'bg-primary' : 'bg-draw'}`}
                  style={{ width: montat ? `${s.procent}%` : '0%' }}
                />
                {/* markerul la 50% = exact media ligii */}
                <span className="absolute inset-y-0 left-1/2 w-px -translate-x-1/2 bg-ink2/40" />
              </div>
              <p className="mt-1 text-[11px] text-ink2">media ligii: {formatNumar(s.medieLiga)}</p>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
