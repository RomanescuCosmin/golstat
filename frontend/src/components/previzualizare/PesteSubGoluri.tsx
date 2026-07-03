import type { LinieGolDto } from '../../api/types';
import { formatProcent } from '../../lib/format';
import { Card } from '../ui/Card';

const LINII_AFISATE = [0.5, 1.5, 2.5, 3.5, 4.5];

interface PesteSubGoluriProps {
  linii: LinieGolDto[];
}

/** Probabilitatile "Peste x.5" goluri pe liniile clasice, colorate dupa marime. */
export function PesteSubGoluri({ linii }: PesteSubGoluriProps) {
  const afisate = LINII_AFISATE.map((l) => linii.find((x) => x.linie === l)).filter(
    (l): l is LinieGolDto => l !== undefined,
  );
  const lista = afisate.length > 0 ? afisate : linii.slice(0, 5);

  return (
    <Card className="h-full p-5">
      <h2 className="text-base font-bold text-ink">Peste/Sub goluri</h2>
      <p className="mt-0.5 text-sm text-ink2">Probabilitate</p>

      {lista.length === 0 ? (
        <p className="mt-4 text-sm text-ink2">Fără date pentru acest meci.</p>
      ) : (
        <div className="mt-4 flex justify-between gap-2">
          {lista.map((linie) => (
            <div key={linie.linie} className="min-w-0 text-center">
              <p className="whitespace-nowrap text-xs text-ink2">Peste {linie.linie}</p>
              <p
                className={`mt-1 text-base font-extrabold sm:text-lg ${
                  linie.peste.procent >= 50 ? 'text-primary' : 'text-accent'
                }`}
              >
                {formatProcent(linie.peste.procent)}
              </p>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
