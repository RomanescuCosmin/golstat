import type { PredictieMeciDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatProcent } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';

interface ProbabilitateRezultatProps {
  predictie: PredictieMeciDto;
}

interface Zona {
  procent: number;
  eticheta: string;
  clasa: string;
  clasaProcent: string;
}

/** Indexul rezultatului real 1X2 (0=gazde, 1=egal, 2=oaspeti); null la meciuri viitoare. */
function indexRezultatReal(predictie: PredictieMeciDto): number | null {
  const r = predictie.rezultat;
  if (!r) return null;
  if (r.goluriGazde > r.goluriOaspeti) return 0;
  if (r.goluriGazde === r.goluriOaspeti) return 1;
  return 2;
}

/** Cele trei zone 1 / X / 2, cu latimi proportionale cu probabilitatea. */
export function ProbabilitateRezultat({ predictie }: ProbabilitateRezultatProps) {
  const zone: Zona[] = [
    {
      procent: predictie.gazde.procent,
      eticheta: numeEchipa(predictie.echipaGazde),
      clasa: 'bg-primary/10 dark:bg-primary/15',
      clasaProcent: 'text-primary',
    },
    {
      procent: predictie.egal.procent,
      eticheta: 'Egal',
      clasa: 'bg-ink2/10 dark:bg-ink2/15',
      clasaProcent: 'text-ink',
    },
    {
      procent: predictie.oaspeti.procent,
      eticheta: numeEchipa(predictie.echipaOaspeti),
      clasa: 'bg-accent/10 dark:bg-accent/15',
      clasaProcent: 'text-accent',
    },
  ];

  const real = indexRezultatReal(predictie);
  // top pick al modelului = zona cu procentul maxim; nimeresc daca == rezultatul real
  const pick = zone.reduce((max, z, i) => (z.procent > zone[max].procent ? i : max), 0);
  const corect = real != null ? pick === real : null;

  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-ink">Probabilitate rezultat</h2>
          <p className="mt-0.5 text-sm text-ink2">Pe baza formei actuale și a statisticilor sezoniere</p>
        </div>
        {corect != null && (
          <Badge variant={corect ? 'win' : 'loss'}>{corect ? '✓ Corect' : '✗ Greșit'}</Badge>
        )}
      </div>

      <div className="mt-4 flex gap-1.5">
        {zone.map((zona, i) => (
          <div
            key={zona.eticheta}
            style={{ flexGrow: Number.isFinite(zona.procent) ? Math.max(zona.procent, 18) : 18, flexBasis: 0 }}
            className={`relative min-w-0 rounded-lg px-2 py-4 text-center ${zona.clasa} ${
              i === real ? 'ring-2 ring-inset ring-ink/40' : ''
            }`}
          >
            {i === real && (
              <span className="absolute right-1.5 top-1.5 text-[11px] font-bold text-ink2">rezultat</span>
            )}
            <p className={`text-xl font-extrabold ${zona.clasaProcent}`}>{formatProcent(zona.procent)}</p>
            <p className={`mt-0.5 truncate text-sm font-medium ${zona.clasaProcent}`}>{zona.eticheta}</p>
          </div>
        ))}
      </div>
    </Card>
  );
}
