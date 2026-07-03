import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, getPredictiiZi } from '../api/client';
import type { PredictieMeciDto } from '../api/types';
import { BandaLive } from '../components/meciuri/BandaLive';
import { BandaZile } from '../components/meciuri/BandaZile';
import { RightSidebar } from '../components/meciuri/RightSidebar';
import { SectiuneCompetitie } from '../components/meciuri/SectiuneCompetitie';
import { SelectorLiga } from '../components/meciuri/SelectorLiga';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';
import { toISODataLocala } from '../lib/format';
import { LIGI, numeLiga } from '../lib/ligi';

export function MeciuriPage() {
  const navigate = useNavigate();
  const [leagueId, setLeagueId] = useState(LIGI[0]!.id);
  const [dataISO, setDataISO] = useState(() => toISODataLocala(new Date()));
  const [meciuri, setMeciuri] = useState<PredictieMeciDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getPredictiiZi(leagueId, dataISO)
      .then((rezultat) => {
        if (!anulat) setMeciuri(rezultat);
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setMeciuri([]);
          setEroare(e instanceof ApiError ? e : new ApiError(0, 'Eroare neașteptată', String(e)));
        }
      })
      .finally(() => {
        if (!anulat) setLoading(false);
      });
    return () => {
      anulat = true;
    };
  }, [leagueId, dataISO, incercare]);

  return (
    <div className="mx-auto flex max-w-7xl items-start gap-5">
      <div className="min-w-0 flex-1">
        <div className="mb-5 flex flex-wrap items-center gap-3">
          <BandaZile selectata={dataISO} onSelect={setDataISO} />
          <div className="ml-auto">
            <SelectorLiga leagueId={leagueId} onChange={setLeagueId} />
          </div>
        </div>

        {loading && (
          <div className="flex justify-center py-20">
            <Spinner size={36} />
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

        {!loading && !eroare && meciuri.length === 0 && (
          <Card>
            <EmptyState
              titlu="Nicio predicție pentru această zi"
              mesaj="Alege altă dată sau altă competiție."
            />
          </Card>
        )}

        {!loading && !eroare && meciuri.length > 0 && (
          <div className="space-y-5">
            <SectiuneCompetitie
              numeLiga={numeLiga(leagueId)}
              regiune={LIGI.find((l) => l.id === leagueId)?.regiune}
              meciuri={meciuri}
              onOpen={(id) => navigate(`/meci/${id}`)}
            />
            <BandaLive
              meciuri={meciuri}
              numeLiga={numeLiga(leagueId)}
              onOpen={(id) => navigate(`/meci/${id}`)}
            />
          </div>
        )}
      </div>

      <RightSidebar meciuri={meciuri} ligaSelectata={leagueId} onAlegeLiga={setLeagueId} />
    </div>
  );
}
