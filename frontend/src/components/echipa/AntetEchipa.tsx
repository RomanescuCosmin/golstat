import type { AntetEchipa as AntetEchipaData, MeciForma, SumarSezon } from '../../api/types';
import { useCountUp } from '../../hooks/useCountUp';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';
import { IconShield, IconUser } from '../ui/icons';
import { FormaRecenta } from './FormaRecenta';
import { SelectorSezon } from './SelectorSezon';

interface AntetEchipaProps {
  antet: AntetEchipaData;
  sumar: SumarSezon | null;
  forma: MeciForma[];
  sezoane: number[];
  sezon: number | null;
  onSezon: (sezon: number) => void;
}

/** Antetul paginii echipei: identitate (stanga), selector de sezon (centru), sumar + forma (dreapta). */
export function AntetEchipa({ antet, sumar, forma, sezoane, sezon, onSezon }: AntetEchipaProps) {
  return (
    <Card className="p-5 sm:p-7">
      <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
        {/* identitate */}
        <div className="flex min-w-0 items-center gap-5">
          <TeamLogo nume={antet.nume} logo={antet.logo} size={96} className="shrink-0" />
          <div className="min-w-0">
            <h1 className="truncate text-2xl font-extrabold leading-tight text-ink sm:text-[32px]">
              {antet.nume ?? `Echipa #${antet.teamId}`}
            </h1>
            <div className="mt-1.5 flex items-center gap-2 text-sm font-medium text-ink2">
              {antet.liga && (
                <LigaLogo id={antet.leagueId ?? undefined} logo={antet.ligaLogo} nume={antet.liga} size={18} />
              )}
              <span className="truncate">{[antet.tara, antet.liga].filter(Boolean).join(' · ')}</span>
            </div>
            <div className="mt-2.5 flex flex-wrap gap-x-4 gap-y-1 text-xs text-ink2">
              {antet.antrenor && (
                <span className="inline-flex items-center gap-1.5">
                  <IconUser width={14} height={14} /> {antet.antrenor}
                </span>
              )}
              {antet.stadion && (
                <span className="inline-flex items-center gap-1.5">
                  <IconShield width={14} height={14} /> {antet.stadion}
                  {antet.capacitate != null && ` · ${antet.capacitate.toLocaleString('ro-RO')} locuri`}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* coloana dreapta: selector sezon deasupra, apoi sumar + forma */}
        <div className="flex flex-col gap-4 xl:shrink-0 xl:items-end">
          <SelectorSezon sezoane={sezoane} valoare={sezon} onChange={onSezon} />
          {sumar && (
            <>
              <div className="grid grid-cols-5 gap-3 sm:gap-6">
                <Cifra valoare={sumar.pozitie != null ? sumar.pozitie : '—'} eticheta="Poziție" subeticheta={antet.liga} accent />
                <Cifra valoare={sumar.puncte} eticheta="Puncte" />
                <Cifra valoare={sumar.victorii} eticheta="Victorii" />
                <Cifra valoare={sumar.egaluri} eticheta="Egaluri" />
                <Cifra valoare={sumar.infrangeri} eticheta="Înfrângeri" />
              </div>
              <div className="flex items-center gap-2.5">
                <span className="text-[11px] font-semibold uppercase tracking-wide text-ink2">Formă</span>
                <FormaRecenta forma={forma} />
              </div>
            </>
          )}
        </div>
      </div>
    </Card>
  );
}

function Cifra({
  valoare,
  eticheta,
  subeticheta,
  accent = false,
}: {
  valoare: number | string | null;
  eticheta: string;
  subeticheta?: string | null;
  accent?: boolean;
}) {
  const numar = useCountUp(typeof valoare === 'number' ? valoare : 0);
  return (
    <div className="text-center">
      <div className={`text-2xl font-extrabold tabular-nums sm:text-3xl ${accent ? 'text-primary' : 'text-ink'}`}>
        {typeof valoare === 'number' ? numar : valoare ?? '—'}
      </div>
      <div className="mt-0.5 text-[10px] font-semibold uppercase tracking-wide text-ink2 sm:text-[11px]">
        {eticheta}
      </div>
      {subeticheta && <div className="truncate text-[10px] text-ink2/70">{subeticheta}</div>}
    </div>
  );
}
