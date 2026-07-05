import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getEchipa } from '../api/client';
import type { PaginaEchipa } from '../api/types';
import { AntetEchipa } from '../components/echipa/AntetEchipa';
import { ClasamentSnippet } from '../components/echipa/ClasamentSnippet';
import { DistributieGoluri } from '../components/echipa/DistributieGoluri';
import { GraficForma } from '../components/echipa/GraficForma';
import { RezultateRecente } from '../components/echipa/RezultateRecente';
import { StatBareSezon } from '../components/echipa/StatBareSezon';
import { StatProcente } from '../components/echipa/StatProcente';
import { TabsEchipa } from '../components/echipa/TabsEchipa';
import { TopJucatori } from '../components/echipa/TopJucatori';
import { UrmatorulMeci } from '../components/echipa/UrmatorulMeci';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton } from '../components/ui/Skeleton';

export function TeamPage() {
  const { teamId } = useParams<{ teamId: string }>();
  const id = Number(teamId);

  const [date, setDate] = useState<PaginaEchipa | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);
  const [sezon, setSezon] = useState<number | null>(null);

  // schimbarea echipei reseteaza sezonul selectat
  useEffect(() => {
    setSezon(null);
  }, [id]);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setEroare(new ApiError(0, 'Echipă invalidă', 'Identificatorul echipei nu este valid.'));
      setLoading(false);
      return;
    }
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getEchipa(id, undefined, sezon ?? undefined)
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
  }, [id, incercare, sezon]);

  return (
    <PageLayout>
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink2" aria-label="Breadcrumb">
        <Link to="/" className="font-medium hover:text-primary">
          Echipe
        </Link>
        <span aria-hidden>›</span>
        <span className="font-semibold text-ink">{date?.antet.nume ?? 'Echipă'}</span>
      </nav>

      {loading && (
        <div className="space-y-5">
          <Card className="flex flex-wrap items-center gap-5 p-6">
            <Skeleton className="h-16 w-16 shrink-0 rounded-full" />
            <div className="min-w-0 flex-1 space-y-2">
              <Skeleton className="h-7 w-56 max-w-full" />
              <Skeleton className="h-4 w-36" />
            </div>
            <Skeleton className="h-9 w-28 rounded-btn" />
          </Card>
          <div className="grid items-start gap-5 lg:grid-cols-2 xl:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <Card key={i} className="space-y-3 p-5">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-3.5 w-full" />
                <Skeleton className="h-3.5 w-5/6" />
                <Skeleton className="h-3.5 w-2/3" />
                <Skeleton className="h-24 w-full" />
              </Card>
            ))}
          </div>
        </div>
      )}

      {!loading && eroare && (
        <Card>
          {eroare.status === 404 ? (
            <ErrorState titlu="Echipa nu a fost găsită" mesaj="Nu există date pentru această echipă." />
          ) : (
            <ErrorState
              titlu={eroare.title}
              mesaj={eroare.detail ?? eroare.message}
              onRetry={() => setIncercare((n) => n + 1)}
            />
          )}
        </Card>
      )}

      {!loading && !eroare && date && (
        <div className="animate-fade-in space-y-5">
          <AntetEchipa
            antet={date.antet}
            sumar={date.sumar}
            forma={date.forma}
            sezoane={date.sezoane}
            sezon={sezon ?? date.antet.sezon}
            onSezon={setSezon}
          />

          <TabsEchipa />

          <div className="grid items-start gap-5 lg:grid-cols-2 xl:grid-cols-3">
            {date.urmatorulMeci ? (
              <UrmatorulMeci meci={date.urmatorulMeci} echipa={{ nume: date.antet.nume, logo: date.antet.logo }} />
            ) : (
              <Card className="p-5">
                <h2 className="text-sm font-extrabold text-ink">Următorul meci</h2>
                <EmptyState titlu="Niciun meci programat" mesaj="Nu există un meci viitor pentru această echipă." />
              </Card>
            )}
            <GraficForma rezultate={date.rezultateRecente} />
            <StatBareSezon statistici={date.statistici} sumar={date.sumar} />
            <StatProcente statProcente={date.statProcente} />
            <ClasamentSnippet randuri={date.clasament} teamId={id} />
            <RezultateRecente rezultate={date.rezultateRecente} />
            <DistributieGoluri buckets={date.goluriPeInterval} />
          </div>

          <TopJucatori top={date.topJucatori} />
        </div>
      )}
    </PageLayout>
  );
}
