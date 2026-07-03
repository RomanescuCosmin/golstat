import type { EchipaDto, FormaEchipaDto, StatisticiCheieDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';

const numar = new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 });

interface Rand {
  eticheta: string;
  gazde: number | null;
  oaspeti: number | null;
  procent?: boolean;
}

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

interface StatisticiCheieProps {
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  statistici: StatisticiCheieDto;
  formaGazde: FormaEchipaDto;
  formaOaspeti: FormaEchipaDto;
}

/** Barele comparative albastru (gazde) vs rosu (oaspeti), cu eticheta pe centru. */
export function StatisticiCheie({ gazde, oaspeti, statistici, formaGazde, formaOaspeti }: StatisticiCheieProps) {
  const randuri: Rand[] = [
    { eticheta: 'Goluri marcate', gazde: formaGazde.goluriMarcatePeMeci, oaspeti: formaOaspeti.goluriMarcatePeMeci },
    { eticheta: 'Goluri primite', gazde: formaGazde.goluriPrimitePeMeci, oaspeti: formaOaspeti.goluriPrimitePeMeci },
    { eticheta: 'Posesie medie', gazde: statistici.gazde.posesieMedie, oaspeti: statistici.oaspeti.posesieMedie, procent: true },
    { eticheta: 'Șuturi pe meci', gazde: statistici.gazde.suturiPeMeci, oaspeti: statistici.oaspeti.suturiPeMeci },
    { eticheta: 'Șuturi pe poartă', gazde: statistici.gazde.suturiPePoarta, oaspeti: statistici.oaspeti.suturiPePoarta },
    { eticheta: 'Cornere pe meci', gazde: statistici.gazde.cornerePeMeci, oaspeti: statistici.oaspeti.cornerePeMeci },
    { eticheta: 'Cartonașe pe meci', gazde: statistici.gazde.cartonasePeMeci, oaspeti: statistici.oaspeti.cartonasePeMeci },
  ];

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">
        Statistici cheie <span className="font-normal text-ink2">(per meci)</span>
      </h2>

      <div className="mt-4 flex items-center justify-between gap-3">
        <span className="flex min-w-0 items-center gap-2 text-sm font-bold text-ink">
          <TeamLogo nume={gazde.nume} logo={gazde.logo} size={22} />
          <span className="truncate">{numeEchipa(gazde)}</span>
        </span>
        <span className="flex min-w-0 items-center gap-2 text-sm font-bold text-ink">
          <span className="truncate">{numeEchipa(oaspeti)}</span>
          <TeamLogo nume={oaspeti.nume} logo={oaspeti.logo} size={22} />
        </span>
      </div>

      <div className="mt-4 space-y-3.5">
        {randuri.map((rand) => (
          <div
            key={rand.eticheta}
            className="grid grid-cols-[2.75rem_1fr_auto_1fr_2.75rem] items-center gap-2 sm:gap-3"
          >
            <span className="text-sm font-semibold tabular-nums text-ink">{fmt(rand.gazde, rand.procent)}</span>
            <div className="flex justify-end">
              <div
                className="h-[5px] rounded-full bg-primary"
                style={{ width: `${latime(rand.gazde, rand.oaspeti)}%` }}
              />
            </div>
            <span className="whitespace-nowrap px-1 text-center text-xs text-ink2 sm:text-sm">{rand.eticheta}</span>
            <div>
              <div
                className="h-[5px] rounded-full bg-accent"
                style={{ width: `${latime(rand.oaspeti, rand.gazde)}%` }}
              />
            </div>
            <span className="text-right text-sm font-semibold tabular-nums text-ink">
              {fmt(rand.oaspeti, rand.procent)}
            </span>
          </div>
        ))}
      </div>
    </Card>
  );
}
