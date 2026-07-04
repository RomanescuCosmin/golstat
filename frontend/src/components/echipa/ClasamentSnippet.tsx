import type { RandClasament } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { TeamLogo } from '../ui/TeamLogo';

interface ClasamentSnippetProps {
  randuri: RandClasament[];
  teamId: number;
  /** Restrange la o fereastra in jurul echipei curente (folosit in rail). */
  compact?: boolean;
}

function fereastra(randuri: RandClasament[], teamId: number, span = 2): RandClasament[] {
  const idx = randuri.findIndex((r) => r.echipaCurenta || r.teamId === teamId);
  if (idx < 0) return randuri.slice(0, span * 2 + 1);
  const start = Math.max(0, idx - span);
  return randuri.slice(start, start + span * 2 + 1);
}

/** "Poziție în clasament": tabel compact (#, echipă, MJ, PCT, DG) cu randul echipei evidentiat. */
export function ClasamentSnippet({ randuri, teamId, compact = false }: ClasamentSnippetProps) {
  const afisate = compact ? fereastra(randuri, teamId) : randuri;

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Poziție în clasament</h2>
      {afisate.length === 0 ? (
        <EmptyState titlu="Fără clasament" mesaj="Nu există date de clasament pentru această ligă." />
      ) : (
        <div className={compact ? '' : 'mt-3 max-h-[26rem] overflow-y-auto'}>
          <table className="mt-3 w-full text-sm">
            <thead>
              <tr className="text-left text-[11px] font-semibold uppercase tracking-wide text-ink2">
                <th className="w-8 pb-2 font-semibold">#</th>
                <th className="pb-2 font-semibold">Echipă</th>
                <th className="w-8 pb-2 text-right font-semibold">MJ</th>
                <th className="w-10 pb-2 text-right font-semibold">PCT</th>
                <th className="w-10 pb-2 text-right font-semibold">DG</th>
              </tr>
            </thead>
            <tbody>
              {afisate.map((r) => {
                const curenta = r.echipaCurenta || r.teamId === teamId;
                return (
                  <tr
                    key={r.teamId}
                    className={curenta ? 'bg-primary/10' : undefined}
                  >
                    <td className="rounded-l-md py-1.5 pl-1.5 tabular-nums text-ink2">{r.rank ?? '—'}</td>
                    <td className="py-1.5">
                      <span className="flex min-w-0 items-center gap-2">
                        <TeamLogo nume={r.nume} logo={r.logo} size={18} />
                        <span className={`truncate ${curenta ? 'font-bold text-ink' : 'text-ink'}`}>
                          {r.nume ?? `#${r.teamId}`}
                        </span>
                      </span>
                    </td>
                    <td className="py-1.5 text-right tabular-nums text-ink2">{r.jucate ?? '—'}</td>
                    <td className="py-1.5 text-right font-semibold tabular-nums text-ink">{r.puncte ?? '—'}</td>
                    <td className="rounded-r-md py-1.5 pr-1.5 text-right tabular-nums text-ink2">
                      {r.golaveraj != null && r.golaveraj > 0 ? `+${r.golaveraj}` : r.golaveraj ?? '—'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}
