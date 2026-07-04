import type { MeciForma } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta } from '../../lib/format';
import { Badge } from '../ui/Badge';

const variantaRezultat = { V: 'win', E: 'draw', I: 'loss' } as const;

interface FormaRecentaProps {
  forma: MeciForma[];
  /** Cate meciuri afisam (cele mai recente primele). */
  numar?: number;
}

/** Sirul de badge-uri V/E/I ale ultimelor meciuri; refolosit in antetul echipei. */
export function FormaRecenta({ forma, numar = 5 }: FormaRecentaProps) {
  const ultimele = forma.slice(0, numar);
  if (ultimele.length === 0) {
    return <span className="text-xs text-ink2">Fără meciuri recente</span>;
  }
  return (
    <div className="flex gap-1.5">
      {ultimele.map((meci) => (
        <Badge
          key={meci.fixtureId}
          variant={variantaRezultat[meci.rezultat]}
          className="h-6 w-6 justify-center rounded px-0"
        >
          <span
            title={`${formatDataScurta(meci.data)} · ${numeEchipa(meci.adversar)} · ${meci.golMarcate ?? '—'}-${meci.golPrimite ?? '—'} (${meci.acasa ? 'acasă' : 'deplasare'})`}
          >
            {meci.rezultat}
          </span>
        </Badge>
      ))}
    </div>
  );
}
