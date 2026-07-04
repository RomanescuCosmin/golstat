import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getLive } from '../../api/client';
import type { MeciLive } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { SectiuneRail } from '../layout/SectiuneRail';
import { Badge } from '../ui/Badge';
import { TeamLogo } from '../ui/TeamLogo';

function etichetaMinut(meci: MeciLive): string {
  if (meci.status === 'HT') {
    return 'Pauză';
  }
  return meci.minut != null ? `${meci.minut}'` : 'LIVE';
}

/** Rail-ul "Live acum": meciurile in desfasurare, fiecare linkand catre propriul Match Center. */
export function LiveAcumRail() {
  const [meciuri, setMeciuri] = useState<MeciLive[]>([]);

  useEffect(() => {
    let anulat = false;
    getLive()
      .then((rezultat) => {
        if (!anulat) setMeciuri(rezultat);
      })
      .catch(() => {
        // fara live rail la eroare — ramanem pe lista goala
      });
    return () => {
      anulat = true;
    };
  }, []);

  return (
    <SectiuneRail titlu="Live acum" linkText="Vezi toate" linkTo="/live">
      {meciuri.length === 0 ? (
        <p className="px-4 py-4 text-xs text-ink2">Niciun meci live momentan.</p>
      ) : (
        <ul className="divide-y divide-line">
          {meciuri.map((m) => (
            <li key={m.fixtureId}>
              <Link
                to={`/meci/${m.fixtureId}/centru`}
                className="flex items-center gap-3 px-4 py-3 transition hover:bg-bg"
              >
                <Badge variant="live">{etichetaMinut(m)}</Badge>
                <span className="min-w-0 flex-1 space-y-1">
                  <span className="flex items-center gap-2 text-xs font-semibold text-ink">
                    <TeamLogo nume={m.gazde.nume} logo={m.gazde.logo} size={16} />
                    <span className="truncate">{numeEchipa(m.gazde)}</span>
                  </span>
                  <span className="flex items-center gap-2 text-xs font-semibold text-ink">
                    <TeamLogo nume={m.oaspeti.nume} logo={m.oaspeti.logo} size={16} />
                    <span className="truncate">{numeEchipa(m.oaspeti)}</span>
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
