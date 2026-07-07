import { useNavigate } from 'react-router-dom';
import type { RandClasament } from '../../api/types';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';
import { IconChevronRight } from '../ui/icons';

interface ClasamentSnippetProps {
  randuri: RandClasament[];
  teamId: number;
  /** Restrange la o fereastra in jurul echipei curente (folosit in rail). */
  compact?: boolean;
}

function fereastra(randuri: RandClasament[], teamId: number, span = 2): RandClasament[] {
  const marime = span * 2 + 1;
  const idx = randuri.findIndex((r) => r.echipaCurenta || r.teamId === teamId);
  if (idx < 0) return randuri.slice(0, marime);
  // la margini fereastra se lipeste de capat, ca sa ramana mereu plina (5 randuri)
  const start = Math.max(0, Math.min(idx - span, randuri.length - marime));
  return randuri.slice(start, start + marime);
}

/** "Poziție în clasament": tabel (#, echipa, MJ, V, E, Î, DG, PCT) cu randul echipei curente evidentiat. */
export function ClasamentSnippet({ randuri, teamId, compact = false }: ClasamentSnippetProps) {
  const navigate = useNavigate();

  if (randuri.length === 0) {
    return null;
  }

  const afisate = compact ? fereastra(randuri, teamId) : randuri;

  return (
    <Card className="p-5">
      <h2 className="text-sm font-extrabold text-ink">Poziție în clasament</h2>

      <div className="mt-2.5 flex items-center gap-2">
        <LigaLogo size={18} nume="Clasament" className="text-ink2" />
        <span className="text-sm font-bold text-ink">Clasament</span>
      </div>

      <div className={compact ? 'mt-3' : 'mt-3 max-h-[26rem] overflow-y-auto'}>
        <table className="w-full table-fixed border-separate border-spacing-y-0.5 text-sm">
          <thead>
            <tr className="text-[11px] font-semibold uppercase tracking-wide text-ink2">
              <th className="w-6 px-1 pb-2 text-left font-semibold">#</th>
              <th className="px-1 pb-2 text-left font-semibold">Echipa</th>
              <th className="w-7 px-0.5 pb-2 text-center font-semibold">MJ</th>
              <th className="w-7 px-0.5 pb-2 text-center font-semibold">V</th>
              <th className="w-7 px-0.5 pb-2 text-center font-semibold">E</th>
              <th className="w-7 px-0.5 pb-2 text-center font-semibold">Î</th>
              <th className="w-9 px-0.5 pb-2 text-center font-semibold">DG</th>
              <th className="w-9 px-1 pb-2 text-center font-semibold">PCT</th>
            </tr>
          </thead>
          <tbody>
            {afisate.map((r, i) => {
              const curenta = r.echipaCurenta || r.teamId === teamId;
              const golaveraj =
                r.golaveraj != null && r.golaveraj > 0 ? `+${r.golaveraj}` : r.golaveraj ?? '—';
              return (
                <tr
                  key={r.teamId}
                  onClick={() => navigate(`/echipa/${r.teamId}`)}
                  className={`cursor-pointer ${
                    curenta
                      ? 'bg-primary/10 font-semibold text-primary'
                      : `${i % 2 === 1 ? 'bg-bg/70' : ''} text-ink transition duration-200 hover:bg-bg`
                  }`}
                >
                  <td className={`rounded-l-btn px-1 py-2 text-xs tabular-nums ${curenta ? '' : 'text-ink'}`}>
                    {r.rank ?? '—'}
                  </td>
                  <td className="px-1 py-2">
                    <span className="flex min-w-0 items-center gap-2">
                      <TeamLogo nume={r.nume} logo={r.logo} size={18} className="shrink-0" />
                      <span className="truncate">{r.nume ?? `#${r.teamId}`}</span>
                    </span>
                  </td>
                  <td className="px-0.5 py-2 text-center text-xs tabular-nums">{r.jucate ?? '—'}</td>
                  <td className="px-0.5 py-2 text-center text-xs tabular-nums">{r.victorii ?? '—'}</td>
                  <td className="px-0.5 py-2 text-center text-xs tabular-nums">{r.egaluri ?? '—'}</td>
                  <td className="px-0.5 py-2 text-center text-xs tabular-nums">{r.infrangeri ?? '—'}</td>
                  <td className="px-0.5 py-2 text-center text-xs tabular-nums">{golaveraj}</td>
                  <td className="rounded-r-btn px-1 py-2 text-center text-xs font-bold tabular-nums">
                    {r.puncte ?? '—'}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <button
        type="button"
        title="În curând"
        className="mt-3 flex w-full items-center justify-center gap-1.5 border-t border-line pt-3.5 text-[13px] font-bold text-primary transition duration-200 hover:opacity-70"
      >
        Vezi clasamentul complet
        <IconChevronRight width={15} height={15} strokeWidth={2.4} />
      </button>
    </Card>
  );
}
