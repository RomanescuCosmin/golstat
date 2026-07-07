import { Link } from 'react-router-dom';
import type { GrupaClasament } from '../../api/types';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';

/** „Group A" → „Grupa A"; pastreaza eticheta originala daca nu urmeaza tiparul. */
function numeGrupa(nume: string): string {
  const m = nume.match(/^Group\s+(.+)$/i);
  return m ? `Grupa ${m[1]}` : nume;
}

/**
 * Clasamentele pe grupe ale unei competitii cu format de grupe (Cupa Mondiala): un tabel compact
 * per grupa, cu primele doua locuri (calificate) marcate discret.
 */
export function GrupeCompetitie({ grupe }: { grupe: GrupaClasament[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {grupe.map((grupa) => (
        <Card key={grupa.nume}>
          <div className="border-b border-line px-4 py-3">
            <h3 className="text-sm font-extrabold text-ink">{numeGrupa(grupa.nume)}</h3>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-[11px] font-semibold uppercase tracking-wide text-ink2">
                <th className="w-7 py-2 pl-4 text-center font-semibold">#</th>
                <th className="py-2 text-left font-semibold">Echipă</th>
                <th className="w-8 py-2 text-center font-semibold" title="Jucate">J</th>
                <th className="w-9 py-2 text-center font-semibold" title="Golaveraj">GD</th>
                <th className="w-8 py-2 pr-4 text-center font-semibold" title="Puncte">P</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {grupa.randuri.map((r, i) => {
                const calificata = i < 2;
                return (
                  <tr key={r.teamId || `rand-${i}`} className="transition duration-200 hover:bg-bg">
                    <td className="py-2 pl-4 text-center">
                      <span
                        className={`inline-flex h-5 w-5 items-center justify-center rounded text-xs font-bold tabular-nums ${
                          calificata ? 'bg-primary/15 text-primary' : 'text-ink2'
                        }`}
                      >
                        {r.rank ?? i + 1}
                      </span>
                    </td>
                    <td className="min-w-0 py-2">
                      <Link
                        to={`/echipa/${r.teamId}`}
                        className="flex min-w-0 items-center gap-2 hover:text-primary"
                      >
                        <TeamLogo nume={r.nume ?? ''} logo={r.logo} size={18} />
                        <span className="truncate font-semibold text-ink">{r.nume ?? 'Echipă'}</span>
                      </Link>
                    </td>
                    <td className="py-2 text-center tabular-nums text-ink2">{r.jucate ?? 0}</td>
                    <td className="py-2 text-center tabular-nums text-ink2">
                      {r.golaveraj != null && r.golaveraj > 0 ? `+${r.golaveraj}` : r.golaveraj ?? 0}
                    </td>
                    <td className="py-2 pr-4 text-center font-bold tabular-nums text-ink">{r.puncte ?? 0}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Card>
      ))}
    </div>
  );
}
