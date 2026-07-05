import type { BucketGoluri } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

interface DistributieGoluriProps {
  buckets: BucketGoluri[];
}

/** "Distribuție goluri": bare grupate pe intervale de 15 min — albastru = marcate, roșu = primite. */
export function DistributieGoluri({ buckets }: DistributieGoluriProps) {
  const maxim = buckets.reduce((m, b) => Math.max(m, b.marcate, b.primite), 0);

  return (
    <Card className="p-5">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-extrabold text-ink">Distribuție goluri</h2>
        <div className="flex items-center gap-3 text-[11px] font-semibold text-ink2">
          <span className="inline-flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-primary" /> Marcate
          </span>
          <span className="inline-flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-accent" /> Primite
          </span>
        </div>
      </div>

      {maxim === 0 ? (
        <EmptyState titlu="Fără goluri" mesaj="Nu există goluri pe intervale pentru această echipă." />
      ) : (
        <div className="mt-5 flex items-end justify-between gap-2">
          {buckets.map((b) => (
            <div key={b.interval} className="flex flex-1 flex-col items-center gap-2">
              {/* inaltime fixa: procentele barelor au nevoie de un parinte cu inaltime definita */}
              <div className="flex h-[120px] w-full items-end justify-center gap-1">
                <Bara valoare={b.marcate} maxim={maxim} culoare="bg-primary" eticheta={`${b.marcate} marcate (${b.interval})`} />
                <Bara valoare={b.primite} maxim={maxim} culoare="bg-accent" eticheta={`${b.primite} primite (${b.interval})`} />
              </div>
              <span className="text-[10px] font-medium text-ink2">{b.interval}</span>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

function Bara({ valoare, maxim, culoare, eticheta }: { valoare: number; maxim: number; culoare: string; eticheta: string }) {
  return (
    <div
      title={eticheta}
      className={`w-2.5 rounded-t-md transition-[height] duration-500 sm:w-3 ${culoare}`}
      style={{ height: `${Math.max((valoare / maxim) * 100, valoare > 0 ? 5 : 0)}%` }}
    />
  );
}
