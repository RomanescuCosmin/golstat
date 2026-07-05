import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, getCompetitie } from '../api/client';
import type { JucatorTop, PaginaCompetitie } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { SelectorLiga } from '../components/meciuri/SelectorLiga';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { SkeletonCard } from '../components/ui/Skeleton';
import { TeamLogo } from '../components/ui/TeamLogo';
import { IconUser } from '../components/ui/icons';
import { numeEchipa } from '../lib/echipa';
import { LIGI_POPULARE, numeLiga } from '../lib/ligi';

/** Card cu un top de jucatori al competitiei (golgheteri / pasatori). */
function CardTopJucatori({ titlu, jucatori }: { titlu: string; jucatori: JucatorTop[] }) {
  return (
    <Card>
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-sm font-extrabold text-ink">{titlu}</h2>
      </div>
      {jucatori.length === 0 ? (
        <EmptyState titlu="Statisticile jucătorilor nu sunt încă disponibile (se colectează)" />
      ) : (
        <ol className="divide-y divide-line">
          {jucatori.map((j, i) => {
            const nume = j.nume ?? 'Jucător necunoscut';
            return (
              <li
                key={j.playerId ?? `rand-${i}`}
                className="flex items-center gap-3 px-5 py-3 transition duration-200 hover:bg-bg"
              >
                <span className="w-5 shrink-0 text-center text-xs font-bold tabular-nums text-ink2">{i + 1}</span>
                <TeamLogo nume={nume} logo={j.foto} size={36} className="rounded-full" />
                <span className="flex min-w-0 flex-1 flex-col">
                  {j.playerId != null ? (
                    <Link
                      to={`/jucator/${j.playerId}`}
                      className="truncate text-sm font-semibold text-ink hover:text-primary"
                    >
                      {nume}
                    </Link>
                  ) : (
                    <span className="truncate text-sm font-semibold text-ink">{nume}</span>
                  )}
                  <span className="flex items-center gap-1.5 text-xs text-ink2">
                    <TeamLogo nume={j.echipa.nume} logo={j.echipa.logo} size={18} />
                    <span className="truncate">{numeEchipa(j.echipa)}</span>
                  </span>
                </span>
                <span className="shrink-0 text-lg font-extrabold tabular-nums text-primary">{j.valoare}</span>
              </li>
            );
          })}
        </ol>
      )}
    </Card>
  );
}

/** Index de jucători: topurile de golgheteri și pasatori ale competiției selectate. */
export function JucatoriPage() {
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

  const optiuniLigi = LIGI_POPULARE.map((id) => ({ id, nume: numeLiga(id) }));

  return (
    <PageLayout>
      <div className="mb-5 flex flex-wrap items-center gap-4">
        <div className="flex min-w-0 flex-1 items-start gap-3">
          <span className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-primary/10 text-primary">
            <IconUser width={20} height={20} />
          </span>
          <div>
            <h1 className="text-2xl font-extrabold text-ink">Jucători</h1>
            <p className="text-sm text-ink2">Topurile jucătorilor din {numeLiga(leagueId)}.</p>
          </div>
        </div>
        <SelectorLiga
          leagueId={leagueId}
          optiuni={optiuniLigi}
          onChange={(v) => setLeagueId(v ?? 39)}
        />
      </div>

      {loading && (
        <div className="grid items-start gap-5 lg:grid-cols-2">
          <SkeletonCard randuri={6} />
          <SkeletonCard randuri={6} />
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

      {!loading && !eroare && date && (
        <div className="animate-fade-in grid items-start gap-5 lg:grid-cols-2">
          <CardTopJucatori titlu="Golgheteri" jucatori={date.golgheteri} />
          <CardTopJucatori titlu="Pase decisive" jucatori={date.pasatori} />
        </div>
      )}
    </PageLayout>
  );
}
