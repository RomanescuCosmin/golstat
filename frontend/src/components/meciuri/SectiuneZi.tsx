import type { KeyboardEvent } from 'react';
import type { LigaZi, MeciZiGrupat } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { numeLiga } from '../../lib/ligi';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';

interface SectiuneZiProps {
  liga: LigaZi;
  onOpen: (fixtureId: number) => void;
}

/** O competitie = O sectiune (stil flashscore): antet o data, apoi meciurile ei ca RANDURI cu scor/ora. */
export function SectiuneZi({ liga, onOpen }: SectiuneZiProps) {
  const nume = liga.nume ?? numeLiga(liga.leagueId);
  return (
    <Card>
      <div className="flex items-center gap-3 border-b border-line px-5 py-3">
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 dark:bg-primary/20">
          <LigaLogo id={liga.leagueId} nume={nume} size={20} />
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-extrabold text-ink">{nume}</p>
          {liga.tara && <p className="truncate text-xs text-ink2">{liga.tara}</p>}
        </div>
        <span className="ml-auto text-xs font-semibold text-ink2">
          {liga.meciuri.length} {liga.meciuri.length === 1 ? 'meci' : 'meciuri'}
        </span>
      </div>

      <div className="divide-y divide-line">
        {liga.meciuri.map((meci) => (
          <RandMeciZi key={meci.fixtureId} meci={meci} onOpen={() => onOpen(meci.fixtureId)} />
        ))}
      </div>
    </Card>
  );
}

/** Eticheta de minut pentru un meci in desfasurare: "Pauză" la HT, altfel "67'". */
function minutText(meci: MeciZiGrupat): string {
  if (meci.status === 'HT') return 'Pauză';
  return meci.minut != null ? `${meci.minut}'` : 'LIVE';
}

/** Un meci ca RAND compact: echipe + (ora la NS / scor live / scor final la terminat). */
function RandMeciZi({ meci, onOpen }: { meci: MeciZiGrupat; onOpen: () => void }) {
  const onKeyDown = (ev: KeyboardEvent<HTMLDivElement>) => {
    if (ev.key === 'Enter' || ev.key === ' ') {
      ev.preventDefault();
      onOpen();
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={onKeyDown}
      className="cursor-pointer px-5 py-3 transition hover:bg-bg focus:bg-bg focus:outline-none"
    >
      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
        <div className="flex min-w-0 items-center justify-end gap-2.5">
          <span className="truncate text-right text-sm font-semibold text-ink">{numeEchipa(meci.gazde)}</span>
          <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={28} />
        </div>

        {meci.inDesfasurare ? (
          <div className="flex flex-col items-center gap-0.5 px-3">
            <span className="text-base font-bold text-accent">
              {meci.golGazde ?? 0} – {meci.golOaspeti ?? 0}
            </span>
            <Badge variant="live">{minutText(meci)}</Badge>
          </div>
        ) : meci.terminat ? (
          <div className="flex flex-col items-center gap-0.5 px-3">
            <span className="text-base font-bold text-ink">
              {meci.golGazde ?? 0} – {meci.golOaspeti ?? 0}
            </span>
            <span className="text-[10px] font-semibold uppercase text-ink2">{meci.status ?? 'FT'}</span>
          </div>
        ) : (
          <span className="px-3 text-base font-bold text-primary">{formatOra(meci.kickoff)}</span>
        )}

        <div className="flex min-w-0 items-center gap-2.5">
          <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={28} />
          <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.oaspeti)}</span>
        </div>
      </div>
    </div>
  );
}
