import type { BucketGoluri } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

interface DistributieGoluriProps {
  buckets: BucketGoluri[];
}

/** "Distribuție goluri": mini histograma pe intervale de 15 minute, bare bg-primary scalate la maxim. */
export function DistributieGoluri({ buckets }: DistributieGoluriProps) {
  const maxim = buckets.reduce((m, b) => Math.max(m, b.goluri), 0);

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Distribuție goluri</h2>
      {maxim === 0 ? (
        <EmptyState titlu="Fără goluri" mesaj="Nu există goluri pe intervale pentru această echipă." />
      ) : (
        <div className="mt-4 flex items-end justify-between gap-1.5" style={{ height: 132 }}>
          {buckets.map((b) => (
            <div key={b.interval} className="flex flex-1 flex-col items-center gap-1.5">
              <span className="text-xs font-semibold tabular-nums text-ink">{b.goluri}</span>
              <div className="flex w-full flex-1 items-end">
                <div
                  className="w-full rounded-t bg-primary"
                  style={{ height: `${Math.max((b.goluri / maxim) * 100, b.goluri > 0 ? 6 : 0)}%` }}
                />
              </div>
              <span className="text-[10px] text-ink2">{b.interval}</span>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
