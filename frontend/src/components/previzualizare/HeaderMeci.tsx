import { Link } from 'react-router-dom';
import type { PredictieMeciDto } from '../../api/types';
import { esteInPlay, minutLive, useLiveScore } from '../../hooks/useLiveScore';
import { numeEchipa } from '../../lib/echipa';
import { formatData, formatOra } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { IconStar } from '../ui/icons';

interface HeaderMeciProps {
  predictie: PredictieMeciDto;
}

/** Antetul previzualizarii: echipele fata in fata, data si ora kickoff-ului (sau scorul live), esantionul de forma. */
export function HeaderMeci({ predictie }: HeaderMeciProps) {
  const { echipaGazde, echipaOaspeti, kickoff, esantionGazde, esantionOaspeti } = predictie;
  const live = useLiveScore(predictie.fixtureId);

  return (
    <Card className="px-5 py-6 sm:px-8">
      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 sm:gap-6">
        <div className="flex items-center justify-center gap-3">
          <IconStar width={18} height={18} className="hidden shrink-0 text-ink2/40 sm:block" />
          <Link
            to={`/echipa/${echipaGazde.id}`}
            className="flex min-w-0 flex-col items-center gap-2 rounded-lg transition hover:opacity-80"
          >
            <TeamLogo nume={echipaGazde.nume} logo={echipaGazde.logo} size={64} />
            <span className="max-w-full truncate text-center text-base font-bold text-ink sm:text-lg">
              {numeEchipa(echipaGazde)}
            </span>
          </Link>
        </div>

        <div className="flex flex-col items-center px-2 text-center">
          {esteInPlay(live) ? (
            <>
              <Badge variant="live">LIVE {minutLive(live)}</Badge>
              <span className="mt-1 text-3xl font-extrabold text-ink sm:text-4xl">
                {live.goalsHome ?? 0} – {live.goalsAway ?? 0}
              </span>
            </>
          ) : (
            <>
              <span className="text-sm font-medium text-ink2">{formatData(kickoff)}</span>
              <span className="mt-1 text-3xl font-extrabold text-ink sm:text-4xl">{formatOra(kickoff)}</span>
            </>
          )}
        </div>

        <div className="flex items-center justify-center gap-3">
          <Link
            to={`/echipa/${echipaOaspeti.id}`}
            className="flex min-w-0 flex-col items-center gap-2 rounded-lg transition hover:opacity-80"
          >
            <TeamLogo nume={echipaOaspeti.nume} logo={echipaOaspeti.logo} size={64} />
            <span className="max-w-full truncate text-center text-base font-bold text-ink sm:text-lg">
              {numeEchipa(echipaOaspeti)}
            </span>
          </Link>
          <IconStar width={18} height={18} className="hidden shrink-0 text-ink2/40 sm:block" />
        </div>
      </div>

      <p className="mt-5 text-center text-xs text-ink2">
        Eșantion: {esantionGazde}/{esantionOaspeti} meciuri
      </p>
    </Card>
  );
}
