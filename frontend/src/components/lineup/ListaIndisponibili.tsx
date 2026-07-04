import type { IndisponibilDto } from '../../api/types';
import { Badge } from '../ui/Badge';

const config = {
  ACCIDENTAT: { variant: 'loss', text: 'Accidentat' },
  SUSPENDAT: { variant: 'loss', text: 'Suspendat' },
  INCERT: { variant: 'draw', text: 'Incert' },
} as const;

interface ListaIndisponibiliProps {
  titlu: string;
  indisponibili: IndisponibilDto[];
}

/** Lista jucatorilor indisponibili: nume + badge dupa motiv + detaliul brut. */
export function ListaIndisponibili({ titlu, indisponibili }: ListaIndisponibiliProps) {
  return (
    <div>
      <h3 className="mb-2 text-xs font-extrabold uppercase tracking-wide text-ink2">{titlu}</h3>
      {indisponibili.length === 0 ? (
        <p className="text-sm text-ink2">Toți disponibili.</p>
      ) : (
        <ul className="space-y-1.5">
          {indisponibili.map((j) => {
            const c = config[j.motiv];
            return (
              <li key={j.id} className="flex items-center justify-between gap-2 text-sm">
                <span className="min-w-0 truncate text-ink">
                  {j.nume ?? '—'}
                  {j.detaliu && <span className="ml-1.5 text-xs text-ink2">({j.detaliu})</span>}
                </span>
                <Badge variant={c.variant}>{c.text}</Badge>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
