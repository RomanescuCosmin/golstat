import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getPrevizualizare } from '../api/client';
import type { PrevizualizareMeciDto } from '../api/types';
import { AmbeleMarcheaza } from '../components/previzualizare/AmbeleMarcheaza';
import { FormaEchipe } from '../components/previzualizare/FormaEchipe';
import { HeaderMeci } from '../components/previzualizare/HeaderMeci';
import { IntalniriDirecte } from '../components/previzualizare/IntalniriDirecte';
import { PesteSubGoluri } from '../components/previzualizare/PesteSubGoluri';
import { ProbabilitateRezultat } from '../components/previzualizare/ProbabilitateRezultat';
import { StatisticiCheie } from '../components/previzualizare/StatisticiCheie';
import { TotalOverUnder } from '../components/previzualizare/TotalOverUnder';
import { Card } from '../components/ui/Card';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';

export function PrevizualizarePage() {
  const { fixtureId } = useParams<{ fixtureId: string }>();
  const id = Number(fixtureId);

  const [date, setDate] = useState<PrevizualizareMeciDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setEroare(new ApiError(0, 'Meci invalid', 'Identificatorul meciului nu este valid.'));
      setLoading(false);
      return;
    }
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getPrevizualizare(id)
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
  }, [id, incercare]);

  return (
    <div className="mx-auto max-w-5xl">
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink2" aria-label="Breadcrumb">
        <Link to="/" className="font-medium hover:text-primary">
          Meciuri
        </Link>
        <span aria-hidden>›</span>
        <span className="font-semibold text-ink">Previzualizare meci</span>
      </nav>

      {loading && (
        <div className="flex justify-center py-20">
          <Spinner size={36} />
        </div>
      )}

      {!loading && eroare && (
        <Card>
          {eroare.status === 404 ? (
            <ErrorState
              titlu="Meciul nu are predicție"
              mesaj="Meciul nu are predicție (nu e viitor)."
            />
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
          <HeaderMeci predictie={date.predictie} />

          <ProbabilitateRezultat predictie={date.predictie} />

          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-[1.7fr_1fr_1fr_1fr]">
            <div className="md:col-span-2 xl:col-span-1">
              <PesteSubGoluri linii={date.predictie.linii} />
            </div>
            <AmbeleMarcheaza btts={date.predictie.btts} />
            <TotalOverUnder titlu="Total cornere" linii={date.cornere} preferate={[8.5, 9.5, 10.5]} />
            <TotalOverUnder titlu="Total cartonașe" linii={date.cartonase} preferate={[3.5, 4.5, 5.5]} />
          </div>

          <StatisticiCheie
            gazde={date.predictie.echipaGazde}
            oaspeti={date.predictie.echipaOaspeti}
            statistici={date.statisticiCheie}
            formaGazde={date.formaGazde}
            formaOaspeti={date.formaOaspeti}
          />

          <div className="grid gap-5 xl:grid-cols-[2fr_3fr]">
            <FormaEchipe
              gazde={date.predictie.echipaGazde}
              oaspeti={date.predictie.echipaOaspeti}
              formaGazde={date.formaGazde}
              formaOaspeti={date.formaOaspeti}
            />
            <IntalniriDirecte intalniri={date.intalniriDirecte} />
          </div>
        </div>
      )}
    </div>
  );
}
