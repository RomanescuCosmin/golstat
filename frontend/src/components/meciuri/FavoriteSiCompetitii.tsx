import { Link } from 'react-router-dom';
import { useFavorite } from '../../hooks/useFavorite';
import { numeEchipa } from '../../lib/echipa';
import { LIGI, LIGI_POPULARE, numeLiga } from '../../lib/ligi';
import { SectiuneRail } from '../layout/SectiuneRail';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';
import { IconStar } from '../ui/icons';

interface FavoriteSiCompetitiiProps {
  /** Liga folosita ca filtru pe prima pagina; `null` = toate competitiile. */
  ligaSelectata: number | null;
  onAlegeLiga: (leagueId: number) => void;
}

/**
 * "Echipe favorite" + "Competiții populare" din rail-ul drept. Favoritele stau pe 2 coloane
 * de tile-uri compacte; fiecare lista e plafonata pe inaltime si are propriul scroll intern.
 */
export function FavoriteSiCompetitii({ ligaSelectata, onAlegeLiga }: FavoriteSiCompetitiiProps) {
  const fav = useFavorite();

  return (
    <>
      <SectiuneRail titlu="Echipe favorite">
        {fav.echipe.length === 0 ? (
          <p className="px-4 py-4 text-xs text-ink2">Apasă ⭐ lângă o echipă ca s-o urmărești aici.</p>
        ) : (
          <ul className="grid max-h-56 grid-cols-2 gap-1.5 overflow-y-auto overscroll-contain p-3 [scrollbar-width:thin]">
            {fav.echipe.map((e) => (
              <li
                key={e.id}
                className="flex items-center gap-2 rounded-xl border border-line/70 bg-bg/50 px-2.5 py-2"
              >
                <Link
                  to={`/echipa/${e.id}`}
                  className="flex min-w-0 flex-1 items-center gap-2 transition hover:text-primary"
                >
                  <TeamLogo nume={e.nume} logo={e.logo} size={18} />
                  <span className="truncate text-xs font-semibold text-ink">{numeEchipa(e)}</span>
                </Link>
                <button
                  type="button"
                  onClick={() => fav.comuta(e)}
                  aria-label={`Scoate ${numeEchipa(e)} din favorite`}
                  className="shrink-0 text-primary transition hover:text-accent"
                >
                  <IconStar width={14} height={14} fill="currentColor" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </SectiuneRail>

      <SectiuneRail titlu="Competiții populare">
        <ul className="max-h-72 divide-y divide-line overflow-y-auto overscroll-contain [scrollbar-width:thin]">
          {LIGI_POPULARE.map((id) => {
            const liga = LIGI.find((l) => l.id === id);
            const activa = id === ligaSelectata;
            return (
              <li key={id}>
                <button
                  type="button"
                  onClick={() => onAlegeLiga(id)}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-left transition hover:bg-bg"
                >
                  <span
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${
                      activa ? 'bg-primary/10 dark:bg-primary/20' : 'bg-ink2/10 dark:bg-ink2/15'
                    }`}
                  >
                    <LigaLogo id={id} nume={numeLiga(id)} size={20} />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className={`block truncate text-sm font-semibold ${activa ? 'text-primary' : 'text-ink'}`}>
                      {numeLiga(id)}
                    </span>
                    {liga?.regiune && <span className="block truncate text-xs text-ink2">{liga.regiune}</span>}
                  </span>
                  <IconStar
                    width={14}
                    height={14}
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
