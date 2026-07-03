import type { KeyboardEvent } from 'react';
import type { PredictieMeciDto } from '../../api/types';
import { esteInPlay, minutLive, useLiveScore } from '../../hooks/useLiveScore';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { IconChart, IconStar } from '../ui/icons';

interface MatchCardProps {
  meci: PredictieMeciDto;
  numeLiga: string;
  onOpen: () => void;
}

/** Card de meci din lista zilei: liga, echipe cu logo, ora (sau scorul live) si mini-indicatorul 1X2. */
export function MatchCard({ meci, numeLiga, onOpen }: MatchCardProps) {
  const live = useLiveScore(meci.fixtureId);
  const g = Math.round(meci.gazde.procent);
  const e = Math.round(meci.egal.procent);
  const o = Math.round(meci.oaspeti.procent);

  const onKeyDown = (ev: KeyboardEvent<HTMLDivElement>) => {
    if (ev.key === 'Enter' || ev.key === ' ') {
      ev.preventDefault();
      onOpen();
    }
  };

  return (
    <Card
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={onKeyDown}
      className="cursor-pointer transition hover:border-primary/40 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary/40"
    >
      <div className="flex items-start justify-between px-5 pt-4">
        <p className="text-sm font-bold text-ink">{numeLiga}</p>
        <IconStar width={18} height={18} className="text-ink2/50" />
      </div>

      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-2 px-5 py-4">
        <div className="flex min-w-0 items-center justify-end gap-3">
          <span className="truncate text-right text-sm font-semibold text-ink sm:text-base">
            {numeEchipa(meci.echipaGazde)}
          </span>
          <TeamLogo nume={meci.echipaGazde.nume} logo={meci.echipaGazde.logo} size={40} />
        </div>

        {esteInPlay(live) ? (
          <div className="flex flex-col items-center gap-1 px-3 sm:px-6">
            <span className="text-xl font-bold text-ink">
              {live.goalsHome ?? 0} – {live.goalsAway ?? 0}
            </span>
            <Badge variant="live">LIVE {minutLive(live)}</Badge>
          </div>
        ) : (
          <span className="px-3 text-xl font-bold text-primary sm:px-6">{formatOra(meci.kickoff)}</span>
        )}

        <div className="flex min-w-0 items-center gap-3">
          <TeamLogo nume={meci.echipaOaspeti.nume} logo={meci.echipaOaspeti.logo} size={40} />
          <span className="truncate text-sm font-semibold text-ink sm:text-base">
            {numeEchipa(meci.echipaOaspeti)}
          </span>
        </div>
      </div>

      <div className="px-5 pb-3">
        <div className="flex h-1.5 overflow-hidden rounded-full bg-line">
          <div className="bg-primary" style={{ width: `${g}%` }} />
          <div className="bg-draw" style={{ width: `${e}%` }} />
          <div className="bg-accent" style={{ width: `${o}%` }} />
        </div>
        <div className="mt-1.5 flex justify-between text-[11px] font-semibold text-ink2">
          <span>
            <span className="text-primary">1</span> {g}%
          </span>
          <span>X {e}%</span>
          <span>
            {o}% <span className="text-accent">2</span>
          </span>
        </div>
      </div>

      <div className="flex items-center justify-center gap-1.5 border-t border-line py-2.5 text-xs font-bold uppercase tracking-wide text-primary">
        <IconChart width={14} height={14} />
        Vezi previzualizare
      </div>
    </Card>
  );
}
