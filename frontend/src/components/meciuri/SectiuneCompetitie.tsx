import type { KeyboardEvent } from 'react';
import type { PredictieMeciDto } from '../../api/types';
import { esteInPlay, minutLive, useLiveScore } from '../../hooks/useLiveScore';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';
import { IconStar } from '../ui/icons';

interface SectiuneCompetitieProps {
  numeLiga: string;
  leagueId?: number;
  regiune?: string;
  meciuri: PredictieMeciDto[];
  onOpen: (fixtureId: number) => void;
}

/** O competitie = O sectiune: antet o singura data, apoi meciurile ei ca RANDURI (nu card per meci). */
export function SectiuneCompetitie({ numeLiga, leagueId, regiune, meciuri, onOpen }: SectiuneCompetitieProps) {
  return (
    <Card>
      <div className="flex items-center gap-3 border-b border-line px-5 py-3">
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 dark:bg-primary/20">
          <LigaLogo id={leagueId} nume={numeLiga} size={20} />
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-extrabold text-ink">{numeLiga}</p>
          {regiune && <p className="truncate text-xs text-ink2">{regiune}</p>}
        </div>
        <span className="ml-auto text-xs font-semibold text-ink2">
          {meciuri.length} {meciuri.length === 1 ? 'meci' : 'meciuri'}
        </span>
      </div>

      <div className="divide-y divide-line">
        {meciuri.map((meci) => (
          <RandMeci key={meci.fixtureId} meci={meci} onOpen={() => onOpen(meci.fixtureId)} />
        ))}
      </div>
    </Card>
  );
}

/** Un meci ca RAND compact in sectiune: echipe + ora (sau scor live) + mini 1X2. */
function RandMeci({ meci, onOpen }: { meci: PredictieMeciDto; onOpen: () => void }) {
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
    <div
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={onKeyDown}
      className="cursor-pointer px-5 py-3 transition hover:bg-bg focus:bg-bg focus:outline-none"
    >
      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
        <div className="flex min-w-0 items-center justify-end gap-2.5">
          <span className="truncate text-right text-sm font-semibold text-ink">{numeEchipa(meci.echipaGazde)}</span>
          <TeamLogo nume={meci.echipaGazde.nume} logo={meci.echipaGazde.logo} size={28} />
        </div>

        {esteInPlay(live) ? (
          <div className="flex flex-col items-center gap-0.5 px-3">
            <span className="text-base font-bold text-ink">
              {live.goalsHome ?? 0} – {live.goalsAway ?? 0}
            </span>
            <Badge variant="live">{minutLive(live)}</Badge>
          </div>
        ) : (
          <span className="px-3 text-base font-bold text-primary">{formatOra(meci.kickoff)}</span>
        )}

        <div className="flex min-w-0 items-center gap-2.5">
          <TeamLogo nume={meci.echipaOaspeti.nume} logo={meci.echipaOaspeti.logo} size={28} />
          <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.echipaOaspeti)}</span>
        </div>
      </div>

      <div className="mt-2 flex items-center gap-2">
        <div className="flex h-1 flex-1 overflow-hidden rounded-full bg-line">
          <div className="bg-primary" style={{ width: `${g}%` }} />
          <div className="bg-draw" style={{ width: `${e}%` }} />
          <div className="bg-accent" style={{ width: `${o}%` }} />
        </div>
        <span className="shrink-0 text-[10px] font-semibold text-ink2">
          <span className="text-primary">1</span> {g}% · X {e}% · <span className="text-accent">2</span> {o}%
        </span>
        <IconStar width={14} height={14} className="shrink-0 text-ink2/40" />
      </div>
    </div>
  );
}
