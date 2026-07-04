import { Link } from 'react-router-dom';
import type { PredictieMeciDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { LIGI } from '../../lib/ligi';
import { SectiuneRail } from '../layout/SectiuneRail';
import { Badge } from '../ui/Badge';
import { TeamLogo } from '../ui/TeamLogo';
import { IconStar, IconTrophy } from '../ui/icons';

interface RightSidebarProps {
  meciuri: PredictieMeciDto[];
  ligaSelectata: number;
  onAlegeLiga: (leagueId: number) => void;
}

/** Continutul rail-ului drept al paginii Meciuri: "LIVE ACUM" + "COMPETIȚII POPULARE". */
export function RightSidebar({ meciuri, ligaSelectata, onAlegeLiga }: RightSidebarProps) {
  // Predictiile sunt pentru meciuri viitoare; "live" = deja incepute (rar, dar posibil in ziua curenta).
  const acum = Date.now();
  const live = meciuri.filter((m) => new Date(m.kickoff).getTime() <= acum);

  return (
    <>
      <SectiuneRail titlu="Live acum" linkText="Vezi toate" linkTo="/live">
        {live.length === 0 ? (
          <p className="px-4 py-4 text-xs text-ink2">Niciun meci live momentan.</p>
        ) : (
          <ul className="divide-y divide-line">
            {live.map((m) => (
              <li key={m.fixtureId}>
                <Link
                  to={`/meci/${m.fixtureId}/centru`}
                  className="flex items-center gap-3 px-4 py-3 transition hover:bg-bg"
                >
                  <Badge variant="live">{formatOra(m.kickoff)}</Badge>
                  <span className="min-w-0 flex-1 space-y-1">
                    <span className="flex items-center gap-2 text-xs font-semibold text-ink">
                      <TeamLogo nume={m.echipaGazde.nume} logo={m.echipaGazde.logo} size={16} />
                      <span className="truncate">{numeEchipa(m.echipaGazde)}</span>
                    </span>
                    <span className="flex items-center gap-2 text-xs font-semibold text-ink">
                      <TeamLogo nume={m.echipaOaspeti.nume} logo={m.echipaOaspeti.logo} size={16} />
                      <span className="truncate">{numeEchipa(m.echipaOaspeti)}</span>
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </SectiuneRail>

      <SectiuneRail titlu="Competiții populare">
        <ul className="divide-y divide-line">
          {LIGI.map((liga) => {
            const activa = liga.id === ligaSelectata;
            return (
              <li key={liga.id}>
                <button
                  type="button"
                  onClick={() => onAlegeLiga(liga.id)}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-bg"
                >
                  <span
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${
                      activa ? 'bg-primary/10 text-primary dark:bg-primary/20' : 'bg-ink2/10 text-ink2 dark:bg-ink2/15'
                    }`}
                  >
                    <IconTrophy width={18} height={18} />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className={`block truncate text-sm font-semibold ${activa ? 'text-primary' : 'text-ink'}`}>
                      {liga.nume}
                    </span>
                    <span className="block text-xs text-ink2">{liga.regiune}</span>
                  </span>
                  <IconStar
                    width={16}
                    height={16}
                    className={activa ? 'shrink-0 text-primary' : 'shrink-0 text-ink2/50'}
                  />
                </button>
              </li>
            );
          })}
        </ul>
      </SectiuneRail>
    </>
  );
}
