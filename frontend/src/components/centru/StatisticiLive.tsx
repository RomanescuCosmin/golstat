import { useState } from 'react';
import type { EchipaDto, StatisticiMeci, StatisticiRepriza } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { BaraComparativa } from '../ui/BaraComparativa';
import { Card } from '../ui/Card';
import { Taburi } from '../ui/Taburi';
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
  /** Reprizele cu date; fara ele nu apar taburile. */
  reprize?: StatisticiRepriza[];
}

/** Statisticile reale dintr-un meci (live sau finalizat), ca bare comparative gazde vs oaspeti; pe meci sau pe repriza. */
export function StatisticiLive({ gazde, oaspeti, statistici, reprize = [] }: StatisticiLiveProps) {
  const [tab, setTab] = useState('meci');
  const repriza = reprize.find((r) => `r${r.repriza}` === tab);
  const sursa = repriza ?? statistici;
  const g = sursa.gazde;
  const o = sursa.oaspeti;

  if (!g && !o) {
    return null;
  }

  const randuri: Rand[] = [
    { eticheta: 'Posesie', gazde: g?.posesie ?? null, oaspeti: o?.posesie ?? null, procent: true },
    { eticheta: 'Șuturi', gazde: g?.suturiTotal ?? null, oaspeti: o?.suturiTotal ?? null },
    { eticheta: 'Șuturi pe poartă', gazde: g?.suturiPePoarta ?? null, oaspeti: o?.suturiPePoarta ?? null },
    { eticheta: 'Cornere', gazde: g?.cornere ?? null, oaspeti: o?.cornere ?? null },
    { eticheta: 'Faulturi', gazde: g?.faulturi ?? null, oaspeti: o?.faulturi ?? null },
    { eticheta: 'Cartonașe galbene', gazde: g?.galbene ?? null, oaspeti: o?.galbene ?? null },
    { eticheta: 'Cartonașe roșii', gazde: g?.rosii ?? null, oaspeti: o?.rosii ?? null },
    { eticheta: 'Pase', gazde: g?.pase ?? null, oaspeti: o?.pase ?? null },
    { eticheta: 'Precizie pase', gazde: g?.preciziePase ?? null, oaspeti: o?.preciziePase ?? null, procent: true },
    { eticheta: 'xG', gazde: g?.xg ?? null, oaspeti: o?.xg ?? null },
  ].filter((rand) => rand.gazde != null || rand.oaspeti != null);

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Statistici meci</h2>

      {reprize.length > 0 && (
        <Taburi
          varianta="pilule"
          className="mt-3"
          activ={repriza ? tab : 'meci'}
          onSchimba={setTab}
          taburi={[
            { id: 'meci', eticheta: 'Meci' },
            ...reprize.map((r) => ({ id: `r${r.repriza}`, eticheta: `Repriza ${r.repriza}` })),
          ]}
        />
      )}

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
