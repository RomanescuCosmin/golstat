import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getLive } from '../../api/client';
import type { MeciLive } from '../../api/types';
import { useFavorite } from '../../hooks/useFavorite';
import { numeEchipa } from '../../lib/echipa';
import { LIGI, LIGI_POPULARE, numeLiga } from '../../lib/ligi';
import { SectiuneRail } from '../layout/SectiuneRail';
import { Badge } from '../ui/Badge';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';
import { IconStar } from '../ui/icons';

const REIMPROSPATARE_MS = 15000;

interface RightSidebarProps {
  /** Liga folosita ca filtru pe prima pagina; `null` = toate competitiile. */
  ligaSelectata: number | null;
  onAlegeLiga: (leagueId: number) => void;
}

/** Continutul rail-ului drept al paginii Meciuri: "LIVE ACUM" (real) + "COMPETIȚII POPULARE" (filtru). */
export function RightSidebar({ ligaSelectata, onAlegeLiga }: RightSidebarProps) {
  const [live, setLive] = useState<MeciLive[]>([]);
  const fav = useFavorite();

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

      <SectiuneRail titlu="Echipe favorite">
        {fav.echipe.length === 0 ? (
          <p className="px-4 py-4 text-xs text-ink2">
            Apasă ⭐ lângă o echipă ca s-o urmărești aici.
          </p>
        ) : (
          <ul className="divide-y divide-line">
            {fav.echipe.map((e) => (
              <li key={e.id} className="flex items-center gap-3 px-4 py-2.5">
                <Link to={`/echipa/${e.id}`} className="flex min-w-0 flex-1 items-center gap-3 transition hover:text-primary">
                  <TeamLogo nume={e.nume} logo={e.logo} size={22} />
                  <span className="truncate text-sm font-semibold text-ink">{numeEchipa(e)}</span>
                </Link>
                <button
                  type="button"
                  onClick={() => fav.comuta(e)}
                  aria-label={`Scoate ${numeEchipa(e)} din favorite`}
                  className="shrink-0 text-primary transition hover:text-accent"
                >
                  <IconStar width={16} height={16} fill="currentColor" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </SectiuneRail>

      <SectiuneRail titlu="Competiții populare">
        <ul className="divide-y divide-line">
          {LIGI_POPULARE.map((id) => {
            const liga = LIGI.find((l) => l.id === id);
            const activa = id === ligaSelectata;
            return (
              <li key={id}>
                <button
                  type="button"
                  onClick={() => onAlegeLiga(id)}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-bg"
                >
                  <span
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${
                      activa ? 'bg-primary/10 dark:bg-primary/20' : 'bg-ink2/10 dark:bg-ink2/15'
                    }`}
                  >
                    <LigaLogo id={id} nume={numeLiga(id)} size={22} />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className={`block truncate text-sm font-semibold ${activa ? 'text-primary' : 'text-ink'}`}>
                      {numeLiga(id)}
                    </span>
                    {liga?.regiune && <span className="block text-xs text-ink2">{liga.regiune}</span>}
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
