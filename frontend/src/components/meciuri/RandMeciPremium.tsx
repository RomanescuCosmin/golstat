import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import type { EchipaDto, MeciZiGrupat } from '../../api/types';
import { useFavorite } from '../../hooks/useFavorite';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { TeamLogo } from '../ui/TeamLogo';
import { IconChart, IconLive, IconStar } from '../ui/icons';
import { BaraProbabilitate } from './BaraProbabilitate';

function minutText(m: MeciZiGrupat): string {
  if (m.status === 'HT') return 'Pauză';
  return m.minut != null ? `${m.minut}'` : 'LIVE';
}

/**
 * Un meci ca RAND premium in cardul competitiei: coloana temporala (ora / minut live / final),
 * echipe cu stele de favorit, scor, bara de probabilitate 1X2 si actiunile Statistici / Preview.
 */
export function RandMeciPremium({ meci }: { meci: MeciZiGrupat }) {
  const navigate = useNavigate();
  const fav = useFavorite();

  const scor =
    meci.golGazde != null || meci.golOaspeti != null
      ? `${meci.golGazde ?? 0} – ${meci.golOaspeti ?? 0}`
      : '–';

  return (
    <div className="group relative rounded-2xl px-3 py-2.5 transition hover:bg-bg">
      <div className="flex items-center gap-3">
        {/* coloana temporala */}
        <div className="flex w-14 shrink-0 flex-col items-center gap-1">
          {meci.inDesfasurare ? (
            <Badge variant="live">{minutText(meci)}</Badge>
          ) : meci.terminat ? (
            <span className="text-[11px] font-semibold uppercase tracking-wide text-ink2">Final</span>
          ) : (
            <span className="text-sm font-bold text-primary tabular-nums">{formatOra(meci.kickoff)}</span>
          )}
        </div>

        {/* echipe + scor */}
        <div className="grid min-w-0 flex-1 grid-cols-[1fr_auto_1fr] items-center gap-2 sm:gap-3">
          <div className="flex min-w-0 items-center justify-end gap-2">
            <SteaFavorit echipa={meci.gazde} este={fav.este(meci.gazde.id)} onComuta={() => fav.comuta(meci.gazde)} />
            <NumeEchipa echipa={meci.gazde} aliniere="dreapta" onOpen={() => navigate(`/echipa/${meci.gazde.id}`)} />
            <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={26} />
          </div>

          <span
            className={`px-2 text-[15px] font-bold tabular-nums ${
              meci.inDesfasurare ? 'text-accent' : meci.terminat ? 'text-ink' : 'text-ink2'
            }`}
          >
            {scor}
          </span>

          <div className="flex min-w-0 items-center gap-2">
            <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={26} />
            <NumeEchipa echipa={meci.oaspeti} aliniere="stanga" onOpen={() => navigate(`/echipa/${meci.oaspeti.id}`)} />
            <SteaFavorit echipa={meci.oaspeti} este={fav.este(meci.oaspeti.id)} onComuta={() => fav.comuta(meci.oaspeti)} />
          </div>
        </div>

        {/* actiuni */}
        <div className="flex shrink-0 items-center gap-1">
          <BtnActiune
            titlu="Statistici"
            onClick={() => navigate(`/meci/${meci.fixtureId}/centru`)}
            icon={<IconChart width={16} height={16} />}
          />
          <BtnActiune
            titlu="Preview"
            onClick={() => navigate(`/meci/${meci.fixtureId}`)}
            icon={<IconLive width={16} height={16} />}
          />
        </div>
      </div>

      {meci.predictie && (
        <div className="mt-2 pl-14 pr-1">
          <BaraProbabilitate predictie={meci.predictie} />
        </div>
      )}
    </div>
  );
}

function NumeEchipa({
  echipa,
  aliniere,
  onOpen,
}: {
  echipa: EchipaDto;
  aliniere: 'stanga' | 'dreapta';
  onOpen: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onOpen}
      className={`truncate text-sm font-semibold text-ink transition hover:text-primary ${
        aliniere === 'dreapta' ? 'text-right' : 'text-left'
      }`}
    >
      {numeEchipa(echipa)}
    </button>
  );
}

function SteaFavorit({ echipa, este, onComuta }: { echipa: EchipaDto; este: boolean; onComuta: () => void }) {
  return (
    <button
      type="button"
      onClick={onComuta}
      aria-label={este ? `Scoate ${numeEchipa(echipa)} din favorite` : `Adaugă ${numeEchipa(echipa)} la favorite`}
      aria-pressed={este}
      className={`shrink-0 transition ${
        este ? 'text-primary' : 'text-ink2/30 opacity-0 group-hover:opacity-100 focus:opacity-100'
      }`}
    >
      <IconStar width={15} height={15} fill={este ? 'currentColor' : 'none'} />
    </button>
  );
}

function BtnActiune({ titlu, onClick, icon }: { titlu: string; onClick: () => void; icon: ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={titlu}
      aria-label={titlu}
      className="flex h-8 w-8 items-center justify-center rounded-full text-ink2 transition hover:bg-primary/10 hover:text-primary"
    >
      {icon}
    </button>
  );
}
