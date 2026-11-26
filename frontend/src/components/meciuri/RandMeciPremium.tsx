import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import type { EchipaDto, MeciZiGrupat } from '../../api/types';
import { useFavorite } from '../../hooks/useFavorite';
import { useMeciuriFavorite } from '../../hooks/useMeciuriFavorite';
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
export function RandMeciPremium({
  meci,
  liga,
}: {
  meci: MeciZiGrupat;
  liga?: { leagueId: number | null; nume: string | null };
}) {
  const navigate = useNavigate();
  const fav = useFavorite();
  const favMeci = useMeciuriFavorite();
  const meciFavorit = favMeci.este(meci.fixtureId);
  const comutaMeci = () =>
    favMeci.comuta({
      fixtureId: meci.fixtureId,
      kickoff: meci.kickoff,
      gazde: meci.gazde,
      oaspeti: meci.oaspeti,
      leagueId: liga?.leagueId ?? null,
      ligaNume: liga?.nume ?? null,
    });

  // la un meci in desfasurare scorul e prin definitie 0-0 chiar daca golurile nu-s inca colectate
  const scor =
    meci.golGazde != null || meci.golOaspeti != null || meci.inDesfasurare
      ? `${meci.golGazde ?? 0} – ${meci.golOaspeti ?? 0}`
      : '–';

  return (
    <div className="group relative px-2 py-3 transition hover:bg-bg sm:rounded-2xl sm:px-3 sm:py-2.5">
      <div className="flex items-center gap-2 sm:gap-3">
        <button
          type="button"
          onClick={comutaMeci}
          aria-label={meciFavorit ? 'Scoate meciul din favorite' : 'Adaugă meciul la favorite'}
          aria-pressed={meciFavorit}
          title={meciFavorit ? 'Scoate din favorite' : 'Adaugă la favorite (sunet la gol)'}
          className={`-m-1.5 shrink-0 p-1.5 transition sm:-ml-1 sm:p-0 sm:m-0 ${meciFavorit ? 'text-accent' : 'text-ink2/40 hover:text-accent'}`}
        >
          <IconStar width={17} height={17} fill={meciFavorit ? 'currentColor' : 'none'} />
        </button>

        {/* coloana temporala */}
        <div className="flex w-12 shrink-0 flex-col items-center gap-1 sm:w-14">
          {meci.inDesfasurare ? (
            <Badge variant="live">{minutText(meci)}</Badge>
          ) : meci.terminat ? (
            <span className="text-[11px] font-semibold uppercase tracking-wide text-ink2">Final</span>
          ) : (
            <span className="text-sm font-bold text-primary tabular-nums">{formatOra(meci.kickoff)}</span>
          )}
        </div>

        {/* echipe + scor */}
        <div className="grid min-w-0 flex-1 grid-cols-[minmax(0,1fr)_auto] grid-rows-2 items-center gap-x-2 gap-y-1.5 border-l border-line/70 pl-3 sm:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] sm:grid-rows-1 sm:gap-3 sm:border-0 sm:pl-0">
          <div className="col-start-1 row-start-1 flex min-w-0 flex-row-reverse items-center justify-end gap-2 sm:flex-row">
            <SteaFavorit echipa={meci.gazde} este={fav.este(meci.gazde.id)} onComuta={() => fav.comuta(meci.gazde)} />
            <NumeEchipa echipa={meci.gazde} aliniere="dreapta" onOpen={() => navigate(`/echipa/${meci.gazde.id}`)} />
            <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={22} />
          </div>

          <span
            className={`col-start-2 row-span-2 row-start-1 rounded-lg bg-bg/70 px-2 py-1 text-center text-[15px] font-bold tabular-nums sm:row-span-1 sm:bg-transparent sm:py-0 ${
              meci.inDesfasurare ? 'text-accent' : meci.terminat ? 'text-ink' : 'text-ink2'
            }`}
          >
            {scor}
          </span>

          <div className="col-start-1 row-start-2 flex min-w-0 items-center gap-2 sm:col-start-3 sm:row-start-1">
            <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={22} />
            <NumeEchipa echipa={meci.oaspeti} aliniere="stanga" onOpen={() => navigate(`/echipa/${meci.oaspeti.id}`)} />
            <SteaFavorit echipa={meci.oaspeti} este={fav.este(meci.oaspeti.id)} onComuta={() => fav.comuta(meci.oaspeti)} />
          </div>
        </div>

        {/* actiuni */}
        <div className="flex shrink-0 flex-col items-center sm:flex-row sm:gap-1">
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
        <div className="mt-2.5 px-1 sm:mt-2 sm:pl-20">
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
      className={`min-w-0 truncate text-left text-sm font-semibold text-ink transition hover:text-primary ${
        aliniere === 'dreapta' ? 'sm:text-right' : ''
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
        este ? 'text-primary' : 'hidden text-ink2/30 opacity-0 group-hover:opacity-100 focus:opacity-100 sm:inline'
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
      className="flex h-9 w-9 items-center justify-center rounded-full text-ink2 transition hover:bg-primary/10 hover:text-primary active:scale-95 sm:h-8 sm:w-8"
    >
      {icon}
    </button>
  );
}
