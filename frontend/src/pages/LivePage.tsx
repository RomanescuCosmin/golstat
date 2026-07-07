import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getLive } from '../api/client';
import type { MeciLive } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { LigaLogo } from '../components/ui/LigaLogo';
import { TeamLogo } from '../components/ui/TeamLogo';
import { useScoreFlash } from '../hooks/useAnimatii';
import { esteInPlay, useLiveScores } from '../hooks/useLiveScore';
import { numeEchipa } from '../lib/echipa';
import { numeLiga } from '../lib/ligi';

const REIMPROSPATARE_MS = 15000;

/**
 * Pagina Live: sursa de meciuri = endpoint-ul real cross-competitii `/v1/meciuri/live` (nu predictiile
 * unei singure ligi), reimprospatat la 15s ca sa prinda meciuri nou-incepute. Scorurile se actualizeaza
 * in timp real prin WebSocket (`/topic/live/{id}`) suprapus peste snapshot-ul din DB.
 */
export function LivePage() {
  const navigate = useNavigate();
  const [live, setLive] = useState<MeciLive[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let anulat = false;
    const incarca = () => {
      getLive()
        .then((r) => {
          if (!anulat) setLive(r);
        })
        .catch(() => {
          if (!anulat) setLive([]);
        })
        .finally(() => {
          if (!anulat) setLoading(false);
        });
    };
    incarca();
    const t = setInterval(incarca, REIMPROSPATARE_MS);
    return () => {
      anulat = true;
      clearInterval(t);
    };
  }, []);

  const scoruri = useLiveScores(live.map((m) => m.fixtureId));

  // Suprapune push-ul WebSocket peste snapshot-ul din DB; un meci ramane afisat cat timp e in-play.
  const afisate = useMemo(
    () =>
      live.map((m) => {
        const push = scoruri[m.fixtureId];
        return esteInPlay(push)
          ? {
              ...m,
              golGazde: push.goalsHome ?? m.golGazde,
              golOaspeti: push.goalsAway ?? m.golOaspeti,
              status: push.statusShort ?? m.status,
              minut: push.statusElapsed ?? m.minut,
            }
          : m;
      }),
    [live, scoruri],
  );

  // Grupare pe competitie, pastrand ordinea din endpoint (cronologica). Meciurile fara liga
  // in DB se strang sub cheia null, nu sub un id fals (altfel titlul ar fi "Liga #0").
  const peLiga = useMemo(() => {
    const map = new Map<number | null, MeciLive[]>();
    for (const m of afisate) {
      const lista = map.get(m.leagueId) ?? [];
      lista.push(m);
      map.set(m.leagueId, lista);
    }
    return [...map.entries()];
  }, [afisate]);

  return (
    <PageLayout>
      <div className="mb-4 flex items-center gap-2">
        <span className="h-2.5 w-2.5 animate-pulse rounded-full bg-accent" />
        <h1 className="text-lg font-bold">Live</h1>
        {afisate.length > 0 && (
          <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-bold text-accent">{afisate.length}</span>
        )}
      </div>

      <p className="mb-4 text-xs text-ink2">
        Apar doar meciurile pentru care API-ul trimite scor live. La competițiile fără acoperire live, rezultatul
        apare pe prima pagină la încheierea meciului.
      </p>

      {loading && (
        <div className="space-y-5">
          {[0, 1].map((i) => (
            <Card key={i} className="p-5">
              <div className="animate-pulse space-y-3 motion-reduce:animate-none">
                <div className="h-4 w-44 rounded bg-ink2/10" />
                <div className="h-11 rounded-lg bg-ink2/10" />
                <div className="h-11 rounded-lg bg-ink2/10" />
              </div>
            </Card>
          ))}
        </div>
      )}

      {!loading && afisate.length === 0 && (
        <Card>
          <EmptyState
            titlu="Niciun meci în desfășurare acum"
            mesaj="Când un meci intră în desfășurare, scorul lui apare aici în timp real."
          />
        </Card>
      )}

      {!loading && peLiga.length > 0 && (
        <div className="space-y-5">
          {peLiga.map(([leagueId, meciuri]) => (
            <SectiuneLive
              key={leagueId ?? 'fara-liga'}
              leagueId={leagueId}
              nume={meciuri[0]?.ligaNume ?? (leagueId != null ? numeLiga(leagueId) : 'Alte competiții')}
              meciuri={meciuri}
              onOpen={(id) => navigate(`/meci/${id}/centru`)}
            />
          ))}
        </div>
      )}
    </PageLayout>
  );
}

function minutText(m: MeciLive): string {
  if (m.status === 'HT') return 'Pauză';
  return m.minut != null ? `${m.minut}'` : 'LIVE';
}

interface SectiuneLiveProps {
  leagueId: number | null;
  nume: string;
  meciuri: MeciLive[];
  onOpen: (fixtureId: number) => void;
}

function SectiuneLive({ leagueId, nume, meciuri, onOpen }: SectiuneLiveProps) {
  return (
    <Card>
      <div className="flex items-center gap-3 border-b border-line px-5 py-3">
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 dark:bg-primary/20">
          <LigaLogo id={leagueId ?? undefined} nume={nume} size={20} />
        </span>
        <p className="truncate text-sm font-extrabold text-ink">{nume}</p>
      </div>

      <div className="divide-y divide-line">
        {meciuri.map((m) => (
          <RandMeciLive key={m.fixtureId} meci={m} onOpen={() => onOpen(m.fixtureId)} />
        ))}
      </div>
    </Card>
  );
}

function RandMeciLive({ meci: m, onOpen }: { meci: MeciLive; onOpen: () => void }) {
  const flash = useScoreFlash(`${m.golGazde ?? 0}-${m.golOaspeti ?? 0}`);
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(ev) => {
        if (ev.key === 'Enter' || ev.key === ' ') {
          ev.preventDefault();
          onOpen();
        }
      }}
      className="grid cursor-pointer grid-cols-[1fr_auto_1fr] items-center gap-3 px-5 py-3 transition hover:bg-bg focus:bg-bg focus:outline-none"
    >
      <div className="flex min-w-0 items-center justify-end gap-2.5">
        <span className="truncate text-right text-sm font-semibold text-ink">{numeEchipa(m.gazde)}</span>
        <TeamLogo nume={m.gazde.nume} logo={m.gazde.logo} size={28} />
      </div>

      <div className="flex flex-col items-center gap-0.5 px-3">
        <span
          className={`text-base font-bold text-accent transition-transform duration-300 motion-reduce:transition-none ${
            flash ? 'scale-125 animate-pulse' : ''
          }`}
        >
          {m.golGazde ?? 0} – {m.golOaspeti ?? 0}
        </span>
        <Badge variant="live">{minutText(m)}</Badge>
      </div>

      <div className="flex min-w-0 items-center gap-2.5">
        <TeamLogo nume={m.oaspeti.nume} logo={m.oaspeti.logo} size={28} />
        <span className="truncate text-sm font-semibold text-ink">{numeEchipa(m.oaspeti)}</span>
      </div>
    </div>
  );
}
