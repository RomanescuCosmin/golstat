import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getProgram } from '../../api/client';
import type { ProgramMeci } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta, formatOra } from '../../lib/format';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { IconCalendar, IconChevronRight } from '../ui/icons';

const LIMITA = 5;

/** Card compact „Urmează": primele meciuri viitoare cross-competitie, cu link spre /program. */
export function CardUrmeaza() {
  const navigate = useNavigate();
  const [meciuri, setMeciuri] = useState<{ meci: ProgramMeci; zi: string }[]>([]);

  useEffect(() => {
    let anulat = false;
    getProgram(7)
      .then((program) => {
        if (anulat) return;
        const plat = program.zile.flatMap((z) => z.ligi.flatMap((l) => l.meciuri.map((m) => ({ meci: m, zi: z.data }))));
        setMeciuri(plat.slice(0, LIMITA));
      })
      .catch(() => {
        if (!anulat) setMeciuri([]);
      });
    return () => {
      anulat = true;
    };
  }, []);

  if (meciuri.length === 0) {
    return null;
  }

  return (
    <Card>
      <div className="flex items-center gap-2 border-b border-line px-5 py-3">
        <IconCalendar width={16} height={16} className="text-primary" />
        <p className="text-sm font-extrabold text-ink">Urmează</p>
        <Link to="/program" className="ml-auto flex items-center gap-0.5 text-xs font-semibold text-primary hover:underline">
          Tot programul
          <IconChevronRight width={14} height={14} />
        </Link>
      </div>
      <div className="divide-y divide-line">
        {meciuri.map(({ meci, zi }) => (
          <button
            key={meci.fixtureId}
            type="button"
            onClick={() => navigate(`/meci/${meci.fixtureId}`)}
            className="flex w-full items-center gap-3 px-5 py-2.5 text-left transition hover:bg-bg focus:bg-bg focus:outline-none"
          >
            <span className="w-24 shrink-0 text-xs font-semibold text-ink2">
              {formatDataScurta(zi)} · {formatOra(meci.kickoff)}
            </span>
            <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={20} />
            <span className="min-w-0 flex-1 truncate text-sm font-semibold text-ink">
              {numeEchipa(meci.gazde)} <span className="text-ink2">–</span> {numeEchipa(meci.oaspeti)}
            </span>
            <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={20} />
          </button>
        ))}
      </div>
    </Card>
  );
}
