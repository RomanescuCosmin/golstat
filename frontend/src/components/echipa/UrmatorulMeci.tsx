import { Link } from 'react-router-dom';
import type { MeciScurt } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatData, formatOra } from '../../lib/format';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { IconCalendar, IconChevronRight } from '../ui/icons';

interface UrmatorulMeciProps {
  meci: MeciScurt;
}

/** Card de rail: urmatorul meci al echipei (adversar, data/ora, acasă/deplasare) cu link la previzualizare. */
export function UrmatorulMeci({ meci }: UrmatorulMeciProps) {
  return (
    <Card className="p-5">
      <h2 className="text-sm font-extrabold uppercase tracking-wide text-ink">Următorul meci</h2>
      <Link to={`/meci/${meci.fixtureId}`} className="group mt-3 flex items-center gap-3">
        <TeamLogo nume={meci.adversar.nume} logo={meci.adversar.logo} size={40} />
        <div className="min-w-0 flex-1">
          <p className="truncate font-bold text-ink group-hover:text-primary">{numeEchipa(meci.adversar)}</p>
          <p className="text-xs font-medium text-ink2">{meci.acasa ? 'Acasă' : 'Deplasare'}</p>
        </div>
        <IconChevronRight width={18} height={18} className="shrink-0 text-ink2/50 group-hover:text-primary" />
      </Link>
      <div className="mt-3 flex items-center gap-2 border-t border-line pt-3 text-sm text-ink2">
        <IconCalendar width={16} height={16} className="shrink-0" />
        <span>
          {formatData(meci.kickoff)} • {formatOra(meci.kickoff)}
        </span>
      </div>
    </Card>
  );
}
