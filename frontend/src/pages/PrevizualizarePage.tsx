import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getPrevizualizare } from '../api/client';
import type { PrevizualizareMeciDto } from '../api/types';
import { FormaEchipe } from '../components/previzualizare/FormaEchipe';
import { HeaderMeci } from '../components/previzualizare/HeaderMeci';
import { IntalniriDirecte } from '../components/previzualizare/IntalniriDirecte';
import { ProbabilitateRezultat } from '../components/previzualizare/ProbabilitateRezultat';
import { SectiuneStatistici } from '../components/previzualizare/SectiuneStatistici';
import { StatisticiCheie } from '../components/previzualizare/StatisticiCheie';
import { EchipaDeStart } from '../components/lineup/EchipaDeStart';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';
import { Taburi, type Tab } from '../components/ui/Taburi';
import { IconChart, IconShield, IconStar, IconTrophy } from '../components/ui/icons';

const TABURI: Tab[] = [
  { id: 'prezentare', eticheta: 'Prezentare', icon: <IconStar width={16} height={16} /> },
  { id: 'rezultate', eticheta: 'Rezultate', icon: <IconTrophy width={16} height={16} /> },
  { id: 'echipe', eticheta: 'Echipe probabile', icon: <IconShield width={16} height={16} /> },
  { id: 'statistici', eticheta: 'Statistici', icon: <IconChart width={16} height={16} /> },
];

export function PrevizualizarePage() {
  const { fixtureId } = useParams<{ fixtureId: string }>();
  const id = Number(fixtureId);

  const [date, setDate] = useState<PrevizualizareMeciDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);
  const [tab, setTab] = useState<string>('prezentare');

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
    <PageLayout>
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
            <div className="flex flex-col items-center gap-3 py-2">
              <ErrorState
                titlu="Meciul nu are predicție"
                mesaj="Predicțiile există doar pentru meciuri viitoare. Dacă meciul e în desfășurare sau s-a jucat, vezi desfășurarea lui."
              />
              <Link
                to={`/meci/${id}/centru`}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90"
              >
                Vezi desfășurarea meciului
              </Link>
            </div>
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

          <Taburi taburi={TABURI} activ={tab} onSchimba={setTab} />

          <div key={tab} className="animate-fade-in space-y-5">
            {tab === 'prezentare' && (
              <>
                <ProbabilitateRezultat predictie={date.predictie} />
                <StatisticiCheie
                  gazde={date.predictie.echipaGazde}
                  oaspeti={date.predictie.echipaOaspeti}
                  statistici={date.statisticiCheie}
                  formaGazde={date.formaGazde}
                  formaOaspeti={date.formaOaspeti}
                />
              </>
            )}

            {tab === 'rezultate' && (
              <div className="grid gap-5 xl:grid-cols-[2fr_3fr]">
                <FormaEchipe
                  gazde={date.predictie.echipaGazde}
                  oaspeti={date.predictie.echipaOaspeti}
                  formaGazde={date.formaGazde}
                  formaOaspeti={date.formaOaspeti}
                />
                <IntalniriDirecte intalniri={date.intalniriDirecte} />
              </div>
            )}

            {tab === 'echipe' &&
              (date.echipeDeStart != null ? (
                <EchipaDeStart
                  echipe={date.echipeDeStart}
                  gazde={date.predictie.echipaGazde}
                  oaspeti={date.predictie.echipaOaspeti}
                />
              ) : (
                <Card className="p-5">
                  <h2 className="text-base font-bold text-ink">Echipe probabile</h2>
                  <EmptyState
                    titlu="Formații indisponibile"
                    mesaj="Echipele probabile nu au fost încă anunțate pentru acest meci."
                  />
                </Card>
              ))}

            {tab === 'statistici' && (
              <SectiuneStatistici
                statistici={date.statistici}
                gazde={date.predictie.echipaGazde}
                oaspeti={date.predictie.echipaOaspeti}
              />
            )}
          </div>
        </div>
      )}
    </PageLayout>
  );
}
