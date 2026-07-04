import { Link } from 'react-router-dom';
import type { FixtureLive, MeciCentral } from '../../api/types';
import { esteInPlay, minutLive } from '../../hooks/useLiveScore';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { numeLiga } from '../../lib/ligi';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';

interface HeaderScorProps {
  meci: MeciCentral;
  live: FixtureLive | null;
}

function etichetaMinut(meci: MeciCentral, live: FixtureLive | null): string {
  if (esteInPlay(live)) {
    return minutLive(live);
  }
  if (meci.status === 'HT') {
    return 'Pauză';
  }
  return meci.minut != null ? `${meci.minut}'` : 'LIVE';
}

/** Antetul Match Center: echipele fata in fata, scorul mare la centru si linia de status (live/final/kickoff). */
export function HeaderScor({ meci, live }: HeaderScorProps) {
  const { gazde, oaspeti, leagueId, kickoff } = meci;
  const inPlay = esteInPlay(live) || meci.inDesfasurare;

  const golGazde = esteInPlay(live) ? live.goalsHome ?? 0 : meci.golGazde ?? 0;
  const golOaspeti = esteInPlay(live) ? live.goalsAway ?? 0 : meci.golOaspeti ?? 0;

  return (
    <Card className="px-5 py-6 sm:px-8">
      {leagueId != null && (
        <div className="mb-5 flex items-center justify-center gap-2 text-xs font-semibold uppercase tracking-wide text-ink2">
          <LigaLogo id={leagueId} logo={meci.ligaLogo} nume={meci.ligaNume} size={18} />
          <span>{meci.ligaNume ?? numeLiga(leagueId)}</span>
        </div>
      )}

      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 sm:gap-6">
        <Link to={`/echipa/${gazde.id}`} className="flex min-w-0 flex-col items-center gap-2 transition hover:opacity-80">
          <TeamLogo nume={gazde.nume} logo={gazde.logo} size={64} />
          <span className="max-w-full truncate text-center text-base font-bold text-ink sm:text-lg">
            {numeEchipa(gazde)}
          </span>
        </Link>

        <div className="flex flex-col items-center px-2 text-center">
          <span className="text-4xl font-extrabold tabular-nums text-ink sm:text-5xl">
            {golGazde} – {golOaspeti}
          </span>
          <span className="mt-2">
            {inPlay ? (
              <Badge variant="live">{etichetaMinut(meci, live)}</Badge>
            ) : meci.terminat ? (
              <Badge variant="neutral">Final</Badge>
            ) : (
              <span className="text-sm font-medium text-ink2">{kickoff ? formatOra(kickoff) : '—'}</span>
            )}
          </span>
        </div>

        <Link to={`/echipa/${oaspeti.id}`} className="flex min-w-0 flex-col items-center gap-2 transition hover:opacity-80">
          <TeamLogo nume={oaspeti.nume} logo={oaspeti.logo} size={64} />
          <span className="max-w-full truncate text-center text-base font-bold text-ink sm:text-lg">
            {numeEchipa(oaspeti)}
          </span>
        </Link>
      </div>

      {(meci.arbitru || meci.stadion) && (
        <p className="mt-5 text-center text-xs text-ink2">
          {[meci.stadion && `Stadion: ${meci.stadion}`, meci.arbitru && `Arbitru: ${meci.arbitru}`]
            .filter(Boolean)
            .join(' · ')}
        </p>
      )}
    </Card>
  );
}
