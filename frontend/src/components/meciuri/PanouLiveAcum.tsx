import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getLive } from '../../api/client';
import type { MeciLive } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { SectiuneRail } from '../layout/SectiuneRail';
import { Badge } from '../ui/Badge';
import { TeamLogo } from '../ui/TeamLogo';

const REIMPROSPATARE_MS = 15000;

/** Sectiunea "LIVE ACUM" din rail-ul drept; lista e plafonata si deruleaza intern. */
export function PanouLiveAcum() {
  const [live, setLive] = useState<MeciLive[]>([]);

  useEffect(() => {
    let anulat = false;
    const incarca = () => {
      getLive()
        .then((r) => {
          if (!anulat) setLive(r);
        })
        .catch(() => {
          if (!anulat) setLive([]);
        });
    };
    incarca();
    const t = setInterval(incarca, REIMPROSPATARE_MS);
    return () => {
      anulat = true;
      clearInterval(t);
    };
  }, []);

  return (
    <SectiuneRail titlu="Live acum" linkText="Vezi toate" linkTo="/live">
      {live.length === 0 ? (
        <p className="px-4 py-4 text-xs text-ink2">Niciun meci live momentan.</p>
      ) : (
        <ul className="max-h-80 divide-y divide-line overflow-y-auto overscroll-contain [scrollbar-width:thin]">
          {live.map((m) => (
            <li key={m.fixtureId}>
              <Link
                to={`/meci/${m.fixtureId}/centru`}
                className="flex items-center gap-3 px-4 py-3 transition hover:bg-bg"
              >
                <Badge variant="live">{m.status === 'HT' ? 'Pauză' : m.minut != null ? `${m.minut}'` : 'LIVE'}</Badge>
                <span className="min-w-0 flex-1 space-y-1">
                  <span className="flex items-center gap-2 text-xs font-semibold text-ink">
                    <TeamLogo nume={m.gazde.nume} logo={m.gazde.logo} size={16} />
                    <span className="truncate">{numeEchipa(m.gazde)}</span>
                    <span className="ml-auto font-bold text-ink">{m.golGazde ?? 0}</span>
                  </span>
                  <span className="flex items-center gap-2 text-xs font-semibold text-ink">
                    <TeamLogo nume={m.oaspeti.nume} logo={m.oaspeti.logo} size={16} />
                    <span className="truncate">{numeEchipa(m.oaspeti)}</span>
                    <span className="ml-auto font-bold text-ink">{m.golOaspeti ?? 0}</span>
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </SectiuneRail>
  );
}
