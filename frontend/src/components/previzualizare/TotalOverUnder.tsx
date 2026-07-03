import type { OverUnder } from '../../api/types';
import { formatRata } from '../../lib/format';
import { Card } from '../ui/Card';

interface TotalOverUnderProps {
  titlu: string;
  linii: OverUnder[];
  /** Liniile pe care le preferam la afisare (ex. 8.5/9.5/10.5 la cornere). */
  preferate: number[];
}

function alegeLinii(linii: OverUnder[], preferate: number[]): OverUnder[] {
  const gasite = preferate
    .map((p) => linii.find((l) => l.line === p))
    .filter((l): l is OverUnder => l !== undefined);
  if (gasite.length > 0) {
    return gasite;
  }
  if (linii.length <= 3) {
    return linii;
  }
  const mijloc = Math.floor(linii.length / 2);
  return linii.slice(mijloc - 1, mijloc + 2);
}

/** Card generic "Total cornere" / "Total cartonașe": cateva linii x.5 cu rata "peste". */
export function TotalOverUnder({ titlu, linii, preferate }: TotalOverUnderProps) {
  const lista = alegeLinii(linii, preferate);

  return (
    <Card className="h-full p-5">
      <h2 className="text-base font-bold text-ink">{titlu}</h2>
      <p className="mt-0.5 text-sm text-ink2">Probabilitate</p>

      {lista.length === 0 ? (
        <p className="mt-4 text-sm text-ink2">Fără date pentru acest meci.</p>
      ) : (
        <div className="mt-4 flex justify-between gap-2">
          {lista.map((linie) => (
            <div key={linie.line} className="min-w-0 text-center">
              <p className="whitespace-nowrap text-xs text-ink2">Peste {linie.line}</p>
              <p
                className={`mt-1 text-base font-extrabold sm:text-lg ${
                  linie.overRate >= 0.5 ? 'text-primary' : 'text-accent'
                }`}
              >
                {formatRata(linie.overRate)}
              </p>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
