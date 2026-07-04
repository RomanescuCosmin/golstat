import { useEffect, useRef, useState } from 'react';
import { getLive } from '../../api/client';
import type { MeciLive } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { numeLiga } from '../../lib/ligi';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { IconChevronLeft } from '../ui/icons';

const REIMPROSPATARE_MS = 15000;

/** Banda orizontala de meciuri IN DESFASURARE (din DB, orice competitie), cu derulare stanga/dreapta. */
export function BandaLive({ onOpen }: { onOpen: (fixtureId: number) => void }) {
  const [live, setLive] = useState<MeciLive[]>([]);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let anulat = false;
    const incarca = () => {
      getLive()
        .then((r) => {
          if (!anulat) setLive(r);
        })
        .catch(() => {
          if (!anulat) setLive([]);
        });
    };
    incarca();
    const t = setInterval(incarca, REIMPROSPATARE_MS);
    return () => {
      anulat = true;
      clearInterval(t);
    };
  }, []);

  const deruleaza = (directie: number) => {
    ref.current?.scrollBy({ left: directie * 280, behavior: 'smooth' });
  };

  return (
    <Card className="mb-5">
      <div className="flex items-center justify-between border-b border-line px-4 py-2.5">
        <h2 className="flex items-center gap-2 text-sm font-extrabold uppercase tracking-wide text-ink">
          <span className="h-2 w-2 animate-pulse rounded-full bg-accent" />
          Meciuri în desfășurare
          {live.length > 0 && (
            <span className="rounded-full bg-accent/10 px-1.5 py-0.5 text-[11px] font-bold text-accent">{live.length}</span>
          )}
        </h2>
        <div className="flex gap-1.5">
          <button
            type="button"
            aria-label="Derulează stânga"
            onClick={() => deruleaza(-1)}
            className="flex h-7 w-7 items-center justify-center rounded-full border border-line text-ink2 transition hover:bg-bg hover:text-ink"
          >
            <IconChevronLeft width={16} height={16} />
          </button>
          <button
            type="button"
            aria-label="Derulează dreapta"
            onClick={() => deruleaza(1)}
            className="flex h-7 w-7 items-center justify-center rounded-full border border-line text-ink2 transition hover:bg-bg hover:text-ink"
          >
            <IconChevronLeft width={16} height={16} className="rotate-180" />
          </button>
        </div>
      </div>

      {live.length === 0 ? (
        <p className="px-4 py-4 text-xs text-ink2">Niciun meci în desfășurare acum.</p>
      ) : (
        <div
          ref={ref}
          className="flex gap-3 overflow-x-auto px-4 py-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {live.map((m) => (
            <MiniLive key={m.fixtureId} meci={m} onOpen={() => onOpen(m.fixtureId)} />
          ))}
        </div>
      )}
    </Card>
  );
}

function minutText(m: MeciLive): string {
  if (m.status === 'HT') return 'Pauză';
  return m.minut != null ? `${m.minut}'` : (m.status ?? 'LIVE');
}

function MiniLive({ meci, onOpen }: { meci: MeciLive; onOpen: () => void }) {
  return (
    <button
      type="button"
      onClick={onOpen}
      className="w-56 shrink-0 rounded-xl border border-line px-3 py-2.5 text-left transition hover:border-primary/40 hover:bg-bg"
    >
      <div className="mb-2 flex items-center justify-between">
        <Badge variant="live">LIVE {minutText(meci)}</Badge>
        <span className="truncate pl-2 text-[11px] text-ink2">
          {meci.ligaNume ?? (meci.leagueId != null ? numeLiga(meci.leagueId) : '')}
        </span>
      </div>
      <Rand echipa={numeEchipa(meci.gazde)} logo={meci.gazde.logo} nume={meci.gazde.nume} gol={meci.golGazde ?? 0} />
      <Rand echipa={numeEchipa(meci.oaspeti)} logo={meci.oaspeti.logo} nume={meci.oaspeti.nume} gol={meci.golOaspeti ?? 0} />
    </button>
  );
}

function Rand({ echipa, logo, nume, gol }: { echipa: string; logo: string | null; nume: string | null; gol: number }) {
  return (
    <div className="flex items-center gap-2 py-0.5">
      <TeamLogo nume={nume} logo={logo} size={18} />
      <span className="min-w-0 flex-1 truncate text-xs font-semibold text-ink">{echipa}</span>
      <span className="text-sm font-bold text-ink">{gol}</span>
    </div>
  );
}
