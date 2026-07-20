import { Link } from 'react-router-dom';
import { numeEchipa } from '../../lib/echipa';
import { formatCota, formatOra, formatRata } from '../../lib/format';
import type { RandPiete } from '../../lib/piete';
import { BaraProbabilitatePiata } from '../ui/BaraProbabilitatePiata';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';

/** Un meci în lista de piețe: ora, competiția, echipele, bara de șansă, procentul și cota. */
export function RandPiataMeci({ rand }: { rand: RandPiete }) {
  const { meci, cota } = rand;
  return (
    <Link
      to={`/meci/${meci.fixtureId}`}
      className="flex items-center gap-3 border-b border-line px-3 py-2.5 transition last:border-b-0 hover:bg-bg"
    >
      <span className="w-11 shrink-0 text-xs font-semibold tabular-nums text-ink2">
        {formatOra(meci.kickoff)}
      </span>
      <LigaLogo id={meci.liga.id} logo={meci.liga.logo} nume={meci.liga.nume} size={18} />

      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="flex min-w-0 items-center gap-1.5">
          <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={18} />
          <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.gazde)}</span>
        </span>
        <span className="flex min-w-0 items-center gap-1.5">
          <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={18} />
          <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.oaspeti)}</span>
        </span>
      </span>

      <span className="hidden w-40 shrink-0 items-center sm:flex">
        <BaraProbabilitatePiata rata={cota.probabilitate} />
      </span>
      <span
        className={`w-12 shrink-0 text-right text-base font-extrabold tabular-nums ${
          cota.probabilitate >= 0.5 ? 'text-primary' : 'text-draw'
        }`}
      >
        {formatRata(cota.probabilitate)}
      </span>
      <span className="w-12 shrink-0 text-right text-xs font-semibold tabular-nums text-ink2">
        {formatCota(cota.cota)}
      </span>
    </Link>
  );
}
