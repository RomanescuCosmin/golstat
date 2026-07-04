import type { EchipaDto, StatisticiMeci } from '../../api/types';
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

interface StatisticiLiveProps {
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  statistici: StatisticiMeci;
}

/** Statisticile reale dintr-un meci (live sau finalizat), ca bare comparative gazde vs oaspeti. */
export function StatisticiLive({ gazde, oaspeti, statistici }: StatisticiLiveProps) {
  const g = statistici.gazde;
  const o = statistici.oaspeti;

  const randuri: Rand[] = [
    { eticheta: 'Posesie', gazde: g.posesie, oaspeti: o.posesie, procent: true },
    { eticheta: 'Șuturi', gazde: g.suturiTotal, oaspeti: o.suturiTotal },
    { eticheta: 'Șuturi pe poartă', gazde: g.suturiPePoarta, oaspeti: o.suturiPePoarta },
    { eticheta: 'Cornere', gazde: g.cornere, oaspeti: o.cornere },
    { eticheta: 'Faulturi', gazde: g.faulturi, oaspeti: o.faulturi },
    { eticheta: 'Cartonașe galbene', gazde: g.galbene, oaspeti: o.galbene },
    { eticheta: 'Cartonașe roșii', gazde: g.rosii, oaspeti: o.rosii },
    { eticheta: 'Pase', gazde: g.pase, oaspeti: o.pase },
    { eticheta: 'Precizie pase', gazde: g.preciziePase, oaspeti: o.preciziePase, procent: true },
    { eticheta: 'xG', gazde: g.xg, oaspeti: o.xg },
  ].filter((rand) => rand.gazde != null || rand.oaspeti != null);

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Statistici meci</h2>

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
