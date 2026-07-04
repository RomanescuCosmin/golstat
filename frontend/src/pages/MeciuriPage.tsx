import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, getMeciuriZi } from '../api/client';
import type { LigaZi } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { BandaLive } from '../components/meciuri/BandaLive';
import { BandaZile } from '../components/meciuri/BandaZile';
import { CardUrmeaza } from '../components/meciuri/CardUrmeaza';
import { RightSidebar } from '../components/meciuri/RightSidebar';
import { SectiuneZi } from '../components/meciuri/SectiuneZi';
import { SelectorLiga } from '../components/meciuri/SelectorLiga';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';
import { toISODataLocala } from '../lib/format';
import { numeLiga } from '../lib/ligi';

export function MeciuriPage() {
  const navigate = useNavigate();
  const [dataISO, setDataISO] = useState(() => toISODataLocala(new Date()));
  const [filtruLiga, setFiltruLiga] = useState<number | null>(null);
  const [ligi, setLigi] = useState<LigaZi[]>([]);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getMeciuriZi(dataISO)
      .then((rezultat) => {
        if (!anulat) setLigi(rezultat.ligi);
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setLigi([]);
          setEroare(e instanceof ApiError ? e : new ApiError(0, 'Eroare neașteptată', String(e)));
        }
      })
      .finally(() => {
        if (!anulat) setLoading(false);
      });
    return () => {
      anulat = true;
    };
  }, [dataISO, incercare]);

  // Click pe o competitie din rail = comuta filtrul (a doua apasare il scoate).
  const comutaFiltru = (leagueId: number) => setFiltruLiga((curent) => (curent === leagueId ? null : leagueId));

  const ligiAfisate = filtruLiga != null ? ligi.filter((l) => l.leagueId === filtruLiga) : ligi;
  const optiuniLigi = ligi.map((l) => ({ id: l.leagueId, nume: l.nume ?? numeLiga(l.leagueId) }));

  return (
    <PageLayout rightRail={<RightSidebar ligaSelectata={filtruLiga} onAlegeLiga={comutaFiltru} />}>
      <div className="mb-5 flex flex-wrap items-center gap-3">
        <BandaZile selectata={dataISO} onSelect={setDataISO} />
        <div className="ml-auto">
          <SelectorLiga leagueId={filtruLiga} onChange={setFiltruLiga} optiuni={optiuniLigi} />
        </div>
      </div>

      <div className="mb-5">
        <CardUrmeaza />
      </div>

      <BandaLive onOpen={(id) => navigate(`/meci/${id}/centru`)} />

      <p className="mb-5 -mt-2 px-1 text-xs text-ink2">
        La unele competiții API-ul nu trimite scor live — meciul apare cu ora de start, iar rezultatul final apare la
        încheiere.
      </p>

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

      {!loading && !eroare && ligiAfisate.length === 0 && (
        <Card>
          <EmptyState
            titlu="Niciun meci în această zi"
            mesaj="Alege altă dată sau altă competiție din lista din dreapta."
          />
        </Card>
      )}

      {!loading && !eroare && ligiAfisate.length > 0 && (
        <div className="space-y-5">
          {ligiAfisate.map((liga) => (
            <SectiuneZi key={liga.leagueId} liga={liga} onOpen={(id) => navigate(`/meci/${id}`)} />
          ))}
        </div>
      )}
    </PageLayout>
  );
}
