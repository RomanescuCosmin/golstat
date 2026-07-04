import { useNavigate } from 'react-router-dom';
import type { MeciForma } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { TeamLogo } from '../ui/TeamLogo';

const variantaRezultat = { V: 'win', E: 'draw', I: 'loss' } as const;

/** Lista rezultatelor recente: dată, loc, adversar, scor, badge V/E/Î; rând → centrul meciului. */
export function RezultateRecente({ rezultate }: { rezultate: MeciForma[] }) {
  const navigate = useNavigate();

  return (
    <Card>
      <div className="border-b border-line px-5 py-3">
        <h2 className="text-base font-bold text-ink">Rezultate recente</h2>
      </div>
      {rezultate.length === 0 ? (
        <EmptyState titlu="Fără rezultate" mesaj="Nu există meciuri recente pentru această echipă." />
      ) : (
        <div className="divide-y divide-line">
          {rezultate.map((m) => (
            <button
              key={m.fixtureId}
              type="button"
              onClick={() => navigate(`/meci/${m.fixtureId}/centru`)}
              className="flex w-full items-center gap-3 px-5 py-2.5 text-left transition hover:bg-bg focus:bg-bg focus:outline-none"
            >
              <span className="w-16 shrink-0 text-xs font-semibold text-ink2">{formatDataScurta(m.data)}</span>
              <span className="w-8 shrink-0 text-[10px] font-bold uppercase text-ink2">{m.acasa ? 'ACA' : 'DEP'}</span>
              <TeamLogo nume={m.adversar.nume} logo={m.adversar.logo} size={22} />
              <span className="min-w-0 flex-1 truncate text-sm font-semibold text-ink">{numeEchipa(m.adversar)}</span>
              <span className="shrink-0 text-sm font-bold tabular-nums text-ink">
                {m.golMarcate ?? '—'}–{m.golPrimite ?? '—'}
              </span>
              <Badge variant={variantaRezultat[m.rezultat]} className="h-6 w-6 shrink-0 justify-center rounded px-0">
                {m.rezultat}
              </Badge>
            </button>
          ))}
        </div>
      )}
    </Card>
  );
}
