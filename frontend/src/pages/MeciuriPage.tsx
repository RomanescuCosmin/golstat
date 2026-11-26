import { Fragment, useEffect, useMemo, useState } from 'react';
import { useStareSesiune } from '../hooks/useStareSesiune';
import { ApiError, getMeciuriZi } from '../api/client';
import type { LigaZi } from '../api/types';
import { CardCompetitie } from '../components/meciuri/CardCompetitie';
import { CaruselLive } from '../components/meciuri/CaruselLive';
import { FavoriteSiCompetitii } from '../components/meciuri/FavoriteSiCompetitii';
import { FiltreMeciuri, type StareFiltre } from '../components/meciuri/FiltreMeciuri';
import { PanouLiveAcum } from '../components/meciuri/PanouLiveAcum';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { SkeletonCard } from '../components/ui/Skeleton';
import { useFavorite } from '../hooks/useFavorite';
import { toISODataLocala } from '../lib/format';
import { sorteazaCompetitii } from '../lib/ligi';

const FILTRE_INITIALE: StareFiltre = { live: false, favorite: false, curand: false };
const CURAND_MS = 2 * 60 * 60 * 1000; // „începe curând" = următoarele 2 ore

export function MeciuriPage() {
  const fav = useFavorite();
  const [dataISO, setDataISO] = useStareSesiune('meciuri.data', () => toISODataLocala(new Date()));
  const [filtruLiga, setFiltruLiga] = useStareSesiune<number | null>('meciuri.liga', null);
  const [filtre, setFiltre] = useStareSesiune<StareFiltre>('meciuri.filtre', FILTRE_INITIALE);
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

  // Aplica filtrul de competitie + comutatoarele (live / favorite / începe curând) la nivel de meci,
  // scoate competitiile ramase fara meciuri, apoi le ordoneaza (top 5 → cupe → restul → amicale).
  const ligiAfisate = useMemo(() => {
    const acum = Date.now();
    const filtrate = ligi
      .filter((l) => filtruLiga == null || l.leagueId === filtruLiga)
      .map((l) => {
        const meciuri = l.meciuri.filter((m) => {
          if (filtre.live && !m.inDesfasurare) return false;
          if (filtre.favorite && !(fav.este(m.gazde.id) || fav.este(m.oaspeti.id))) return false;
          if (filtre.curand) {
            const t = new Date(m.kickoff).getTime();
            if (m.inDesfasurare || m.terminat || t < acum || t - acum > CURAND_MS) return false;
          }
          return true;
        });
        return { ...l, meciuri };
      })
      .filter((l) => l.meciuri.length > 0);
    return sorteazaCompetitii(filtrate);
  }, [ligi, filtruLiga, filtre, fav]);

  const totalMeciuri = ligiAfisate.reduce((n, l) => n + l.meciuri.length, 0);

  // Coloana stanga curge liber (caruselul live, cand exista meciuri, e al doilea card — dupa
  // primul campionat); railul drept e sticky si deruleaza intern,
  // deci nu poate impinge continutul in jos — inaltimea paginii e data strict de continut.
  return (
    <div className="mx-auto grid max-w-[1640px] grid-cols-1 items-start gap-5 lg:gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
      <div className="min-w-0">
        <h1 className="sr-only">Meciuri</h1>
        <FiltreMeciuri
          dataISO={dataISO}
          onData={setDataISO}
          liga={filtruLiga}
          onLiga={setFiltruLiga}
          filtre={filtre}
          onFiltre={setFiltre}
        />

        <p className="mb-4 -mt-1 px-1 text-[11px] leading-snug text-ink2 sm:text-xs">
          Bara de sub fiecare meci arată șansa reală 1X2. La unele competiții API-ul nu trimite scor live —
          meciul apare cu ora de start, iar rezultatul final apare la încheiere.
        </p>

        {loading && (
          <div className="space-y-3 sm:space-y-[18px]">
            <SkeletonCard randuri={5} />
            <SkeletonCard randuri={4} />
          </div>
        )}

        {!loading && eroare && (
          <Card>
            <ErrorState titlu={eroare.title} mesaj={eroare.detail ?? eroare.message} onRetry={() => setIncercare((n) => n + 1)} />
          </Card>
        )}

        {!loading && !eroare && totalMeciuri === 0 && (
          <div className="space-y-[18px]">
            <Card>
              <EmptyState
                titlu="Niciun meci de afișat"
                mesaj="Schimbă ziua, competiția sau dezactivează filtrele din dreapta sus."
              />
            </Card>
            <CaruselLive />
          </div>
        )}

        {!loading && !eroare && totalMeciuri > 0 && (
          <div className="animate-fade-in space-y-3 sm:space-y-[18px]">
            {ligiAfisate.map((liga, i) => (
              <Fragment key={liga.leagueId}>
                <CardCompetitie liga={liga} />
                {i === 0 && <CaruselLive />}
              </Fragment>
            ))}
          </div>
        )}
      </div>

      {/* Fiecare sectiune din rail isi plafoneaza lista si deruleaza intern (rotita mouse-ului),
          deci oricat continut ar avea rail-ul nu impinge pagina in jos. */}
      <aside className="grid min-w-0 gap-5 md:grid-cols-2 xl:grid-cols-1">
        <PanouLiveAcum />
        <FavoriteSiCompetitii
          ligaSelectata={filtruLiga}
          onAlegeLiga={(id) => setFiltruLiga((c) => (c === id ? null : id))}
        />
      </aside>
    </div>
  );
}
