import type { PredictieMeciDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatProcent } from '../../lib/format';
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

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Probabilitate rezultat</h2>
      <p className="mt-0.5 text-sm text-ink2">Pe baza formei actuale și a statisticilor sezoniere</p>

      <div className="mt-4 flex gap-1.5">
        {zone.map((zona) => (
          <div
            key={zona.eticheta}
            style={{ flexGrow: Math.max(zona.procent, 18), flexBasis: 0 }}
            className={`min-w-0 rounded-lg px-2 py-4 text-center ${zona.clasa}`}
          >
            <p className={`text-xl font-extrabold ${zona.clasaProcent}`}>{formatProcent(zona.procent)}</p>
            <p className={`mt-0.5 truncate text-sm font-medium ${zona.clasaProcent}`}>{zona.eticheta}</p>
          </div>
        ))}
      </div>
    </Card>
  );
}
