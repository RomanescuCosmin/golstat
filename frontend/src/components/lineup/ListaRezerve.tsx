import type { JucatorLineupDto } from '../../api/types';

interface ListaRezerveProps {
  titlu: string;
  rezerve: JucatorLineupDto[];
}

/** Lista de rezerve a unei echipe (numar + nume). */
export function ListaRezerve({ titlu, rezerve }: ListaRezerveProps) {
  return (
    <div>
      <h3 className="mb-2 text-xs font-extrabold uppercase tracking-wide text-ink2">{titlu}</h3>
      {rezerve.length === 0 ? (
        <p className="text-sm text-ink2">Fără rezerve.</p>
      ) : (
        <ul className="space-y-1">
          {rezerve.map((j, i) => (
            <li key={j.id ?? i} className="flex items-center gap-2 text-sm text-ink">
              <span className="w-6 shrink-0 text-right font-semibold tabular-nums text-ink2">
                {j.numar ?? '–'}
              </span>
              <span className="truncate">{j.nume ?? '—'}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
