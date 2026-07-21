import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, getCompetitie } from '../api/client';
import type { PaginaCompetitie } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { SelectorLiga } from '../components/meciuri/SelectorLiga';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton } from '../components/ui/Skeleton';
import { TeamLogo } from '../components/ui/TeamLogo';
import { IconShield } from '../components/ui/icons';
import { numeLiga, optiuniSelectorLigi } from '../lib/ligi';

/** Index de echipe: gridul echipelor din clasamentul competiției selectate. */
export function EchipePage() {
  const [leagueId, setLeagueId] = useState<number>(39);
  const [date, setDate] = useState<PaginaCompetitie | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getCompetitie(leagueId)
      .then((rezultat) => {
        if (!anulat) setDate(rezultat);
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setDate(null);
          setEroare(e instanceof ApiError ? e : new ApiError(0, 'Eroare neașteptată', String(e)));
        }
      })
      .finally(() => {
        if (!anulat) setLoading(false);
      });
    return () => {
      anulat = true;
    };
  }, [leagueId, incercare]);

  const optiuniLigi = optiuniSelectorLigi();

  return (
    <PageLayout>
      <div className="mb-5 flex flex-wrap items-center gap-4">
        <div className="flex min-w-0 flex-1 items-start gap-3">
          <span className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-primary/10 text-primary">
            <IconShield width={20} height={20} />
          </span>
          <div>
            <h1 className="text-2xl font-extrabold text-ink">Echipe</h1>
            <p className="text-sm text-ink2">Echipele din {numeLiga(leagueId)}.</p>
          </div>
        </div>
        <SelectorLiga
          leagueId={leagueId}
          optiuni={optiuniLigi}
          onChange={(v) => setLeagueId(v ?? 39)}
        />
      </div>

      {loading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }, (_, i) => (
            <Card key={i} className="flex items-center gap-3 p-4">
              <Skeleton className="h-9 w-9 shrink-0 rounded-full" />
              <div className="min-w-0 flex-1 space-y-1.5">
                <Skeleton className={`h-3.5 ${i % 2 === 0 ? 'w-3/4' : 'w-3/5'}`} />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </Card>
          ))}
        </div>
      )}

      {!loading && eroare && (
        <Card>
          <ErrorState
            titlu={eroare.title}
            mesaj={eroare.detail ?? eroare.message}
            onRetry={() => setIncercare((n) => n + 1)}
          />
        </Card>
      )}

      {!loading && !eroare && date && date.clasament.length === 0 && (
        <Card>
          <EmptyState
            titlu="Nu există echipe de afișat"
            mesaj="Clasamentul acestei competiții nu este încă disponibil."
          />
        </Card>
      )}

      {!loading && !eroare && date && date.clasament.length > 0 && (
        <div className="animate-fade-in grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {date.clasament.map((r) => (
            <Link key={r.teamId} to={`/echipa/${r.teamId}`} className="group block">
              <Card className="flex items-center gap-3 p-4 transition duration-200 hover:-translate-y-0.5 hover:shadow-cardHover">
                <span className="w-6 shrink-0 text-center text-xs font-bold tabular-nums text-ink2">
                  {r.rank != null ? `#${r.rank}` : '—'}
                </span>
                <TeamLogo nume={r.nume} logo={r.logo} size={36} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold text-ink group-hover:text-primary">
                    {r.nume ?? `Echipa #${r.teamId}`}
                  </span>
                  <span className="block text-xs tabular-nums text-ink2">
                    {r.puncte ?? '—'} pct · {r.jucate ?? '—'} MJ
                  </span>
                </span>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </PageLayout>
  );
}
