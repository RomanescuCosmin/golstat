import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getEchipa } from '../api/client';
import type { PaginaEchipa } from '../api/types';
import { AntetEchipa } from '../components/echipa/AntetEchipa';
import { ClasamentSnippet } from '../components/echipa/ClasamentSnippet';
import { DistributieGoluri } from '../components/echipa/DistributieGoluri';
import { GraficForma } from '../components/echipa/GraficForma';
import { RezultateRecente } from '../components/echipa/RezultateRecente';
import { SelectorSezon } from '../components/echipa/SelectorSezon';
import { StatBareSezon } from '../components/echipa/StatBareSezon';
import { StatProcente } from '../components/echipa/StatProcente';
import { TabsEchipa } from '../components/echipa/TabsEchipa';
import { TopJucatori } from '../components/echipa/TopJucatori';
import { UrmatorulMeci } from '../components/echipa/UrmatorulMeci';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';

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

  const rightRail =
    date && !loading && !eroare ? (
      <>
        {date.urmatorulMeci && <UrmatorulMeci meci={date.urmatorulMeci} />}
        {date.clasament.length > 0 && <ClasamentSnippet randuri={date.clasament} teamId={id} compact />}
      </>
    ) : undefined;

  return (
    <PageLayout rightRail={rightRail}>
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink2" aria-label="Breadcrumb">
        <Link to="/" className="font-medium hover:text-primary">
          Echipe
        </Link>
        <span aria-hidden>›</span>
        <span className="font-semibold text-ink">{date?.antet.nume ?? 'Echipă'}</span>
      </nav>

      {loading && (
        <div className="flex justify-center py-20">
          <Spinner size={36} />
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
        <div className="space-y-5">
          <AntetEchipa antet={date.antet} sumar={date.sumar} forma={date.forma} />

          <div className="flex items-center justify-between gap-3">
            <TabsEchipa />
            <div className="shrink-0">
              <SelectorSezon sezoane={date.sezoane} valoare={sezon ?? date.antet.sezon} onChange={setSezon} />
            </div>
          </div>

          <div className="grid gap-5 lg:grid-cols-2">
            <GraficForma rezultate={date.rezultateRecente} />
            <StatProcente statProcente={date.statProcente} />
            <StatBareSezon statistici={date.statistici} />
            <RezultateRecente rezultate={date.rezultateRecente} />
          </div>

          <DistributieGoluri buckets={date.goluriPeInterval} />

          <TopJucatori top={date.topJucatori} />
        </div>
      )}
    </PageLayout>
  );
}
