import { Link } from 'react-router-dom';
import { numeEchipa } from '../../lib/echipa';
import { formatCota, formatOra, formatRata } from '../../lib/format';
import type { RandPiete } from '../../lib/piete';
import { BaraProbabilitatePiata } from '../ui/BaraProbabilitatePiata';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';

interface Props {
  rand: RandPiete;
  /** Eticheta lungă a pieței alese („Peste 2.5 goluri"), aceeași pe tot rândul. */
  piata: string;
  favorit: boolean;
  onFavorit: (fixtureId: number) => void;
}

function Steluta({ plina }: { plina: boolean }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      width={18}
      height={18}
      fill={plina ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinejoin="round"
    >
      <path d="M12 3.6l2.6 5.3 5.8.85-4.2 4.1 1 5.75L12 16.9l-5.2 2.7 1-5.75-4.2-4.1 5.8-.85z" />
    </svg>
  );
}

/**
 * Un meci în lista de piețe: ora, competiția, echipele, piața aleasă, bara de șansă, procentul
 * și cota. Bara verde e singurul indicator vizual al rândului, deci scanarea listei se face pe
 * lungimea ei; restul cifrelor dau precizia.
 *
 * Pe desktop rândul e o grilă (steluța trece ultima prin `order`), pe mobil devine un card
 * stivuit — de aceea grupurile de mai jos se dizolvă în grilă cu `md:contents`.
 */
export function RandPiataMeci({ rand, piata, favorit, onFavorit }: Props) {
  const { meci, cota } = rand;
  const gazde = numeEchipa(meci.gazde);
  const oaspeti = numeEchipa(meci.oaspeti);
  return (
    <Link
      to={`/meci/${meci.fixtureId}`}
      className="flex flex-col gap-3 rounded-input border border-line bg-card p-3.5 shadow-card transition duration-200 hover:-translate-y-px hover:border-ink2/30 hover:shadow-cardHover dark:shadow-none md:grid md:grid-cols-[3.25rem_1.5rem_minmax(0,1fr)_8.5rem_7rem_2.75rem_3.25rem_2rem] md:items-center md:gap-4 md:p-4"
    >
      <div className="flex items-center gap-2 md:contents">
        <span className="shrink-0 text-xs font-semibold tabular-nums text-ink2">
          {formatOra(meci.kickoff)}
        </span>
        <LigaLogo id={meci.liga.id} logo={meci.liga.logo} nume={meci.liga.nume} size={18} />
        <span className="truncate text-xs text-ink2 md:hidden">{meci.liga.nume}</span>
        <button
          type="button"
          aria-pressed={favorit}
          aria-label={`Salvează ${gazde} – ${oaspeti}`}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            onFavorit(meci.fixtureId);
          }}
          className={`ml-auto flex h-8 w-8 shrink-0 items-center justify-center rounded-full transition md:order-last md:ml-0 md:justify-self-end ${
            favorit ? 'text-piata' : 'text-ink2/60 hover:bg-bg hover:text-ink2'
          }`}
        >
          <Steluta plina={favorit} />
        </button>
      </div>

      <span className="flex min-w-0 flex-col gap-1">
        <span className="flex min-w-0 items-center gap-1.5">
          <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={18} />
          <span className="truncate text-sm font-semibold text-ink">{gazde}</span>
        </span>
        <span className="flex min-w-0 items-center gap-1.5">
          <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={18} />
          <span className="truncate text-sm font-semibold text-ink">{oaspeti}</span>
        </span>
      </span>

      <div className="flex items-center gap-3 md:contents">
        <span className="shrink-0 truncate rounded-full border border-line bg-bg px-2.5 py-1 text-[11px] font-semibold text-ink2">
          {piata}
        </span>
        <BaraProbabilitatePiata rata={cota.probabilitate} varianta="sansa" />
        <span className="shrink-0 text-right text-base font-extrabold tabular-nums text-ink md:w-full">
          {formatRata(cota.probabilitate)}
        </span>
        <span className="shrink-0 rounded-btn border border-line px-2 py-1 text-center text-xs font-semibold tabular-nums text-ink2 md:w-full">
          {formatCota(cota.cota)}
        </span>
      </div>
    </Link>
  );
}
