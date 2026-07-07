import type { EchipaDto, FormaEchipaDto, StatisticiCheieDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { BaraComparativa } from '../ui/BaraComparativa';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';

interface Rand {
  eticheta: string;
  gazde: number | null;
  oaspeti: number | null;
  procent?: boolean;
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
    {
      eticheta: 'Goluri marcate',
      gazde: formaGazde.locatie.goluriMarcatePeMeci,
      oaspeti: formaOaspeti.locatie.goluriMarcatePeMeci,
    },
    {
      eticheta: 'Goluri primite',
      gazde: formaGazde.locatie.goluriPrimitePeMeci,
      oaspeti: formaOaspeti.locatie.goluriPrimitePeMeci,
    },
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
          <BaraComparativa
            key={rand.eticheta}
            eticheta={rand.eticheta}
            gazde={rand.gazde}
            oaspeti={rand.oaspeti}
            procent={rand.procent}
          />
        ))}
      </div>
    </Card>
  );
}
