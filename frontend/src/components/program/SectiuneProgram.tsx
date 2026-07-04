import { useNavigate } from 'react-router-dom';
import type { ProgramLiga, ProgramZi } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatData, formatOra } from '../../lib/format';
import { numeLiga } from '../../lib/ligi';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';

/** O zi de program: antet cu data, apoi cate un card per competitie cu meciurile ei ca randuri. */
export function SectiuneProgram({ zi }: { zi: ProgramZi }) {
  return (
    <section className="space-y-3">
      <h2 className="px-1 text-sm font-extrabold uppercase tracking-wide text-ink2">{formatData(zi.data)}</h2>
      <div className="space-y-4">
        {zi.ligi.map((liga) => (
          <CardCompetitie key={liga.leagueId} liga={liga} />
        ))}
      </div>
    </section>
  );
}

function CardCompetitie({ liga }: { liga: ProgramLiga }) {
  const navigate = useNavigate();
  const nume = liga.nume ?? numeLiga(liga.leagueId);
  return (
    <Card>
      <div className="flex items-center gap-3 border-b border-line px-5 py-3">
        <LigaLogo id={liga.leagueId} logo={liga.logo} nume={liga.nume} size={22} className="text-ink2" />
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
          <button
            key={meci.fixtureId}
            type="button"
            onClick={() => navigate(`/meci/${meci.fixtureId}`)}
            className="grid w-full grid-cols-[1fr_auto_1fr] items-center gap-3 px-5 py-3 text-left transition hover:bg-bg focus:bg-bg focus:outline-none"
          >
            <div className="flex min-w-0 items-center justify-end gap-2.5">
              <span className="truncate text-right text-sm font-semibold text-ink">{numeEchipa(meci.gazde)}</span>
              <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={26} />
            </div>
            <span className="px-3 text-sm font-bold text-primary">{formatOra(meci.kickoff)}</span>
            <div className="flex min-w-0 items-center gap-2.5">
              <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={26} />
              <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.oaspeti)}</span>
            </div>
          </button>
        ))}
      </div>
    </Card>
  );
}
