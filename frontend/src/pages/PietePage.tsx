import { useEffect, useMemo, useState } from 'react';
import { useStareSesiune } from '../hooks/useStareSesiune';
import { ApiError, getPieteZile, type FiltruCompetitie, type FiltruLocatie, type Perioada } from '../api/client';
import { ComutatorCompetitie } from '../components/ui/ComutatorCompetitie';
import { ComutatorLocatie } from '../components/ui/ComutatorLocatie';
import { CalendarPerioada } from '../components/ui/CalendarPerioada';
import { citesteFiltruCompetitie, citesteFiltruLocatie, salveazaFiltruCompetitie, salveazaFiltruLocatie } from '../lib/filtruCompetitie';
import type { CodPiata, PieteZile } from '../api/types';
import { FiltrePiete } from '../components/piete/FiltrePiete';
import { SectiuneZiPiete } from '../components/piete/SectiuneZiPiete';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton } from '../components/ui/Skeleton';
import { useFavoritePiete } from '../hooks/useFavoritePiete';
import {
  codValid,
  etichetaPiata,
  filtreaza,
  intervalValid,
  ligiDisponibile,
  ligiValide,
  linieValida,
  numaraRanduri,
} from '../lib/piete';

/** Data locală ISO (YYYY-MM-DD), decalată cu `zile`. */
function dataIso(zile = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + zile);
  const z = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}

/** Perioada maximă acceptată de server (zile). */
const MAX_ZILE = 14;

/**
 * Lista de piețe pe zilele următoare: alegi o piață (ex. peste 2.5 goluri) și vezi toate meciurile
 * care trec pragul, grupate pe zi și sortate descrescător.
 *
 * Datele se aduc O SINGURĂ DATĂ la montare; schimbarea piețelor și tragerea pragului filtrează
 * local, ca slider-ul să răspundă instant.
 */
export function PietePage() {
  const [date, setDate] = useState<PieteZile | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);
  const [competitie, setCompetitie] = useState<FiltruCompetitie>(citesteFiltruCompetitie);
  const [locatie, setLocatie] = useState<FiltruLocatie>(citesteFiltruLocatie);
  const [perioada, setPerioada] = useStareSesiune<Perioada>('piete.perioada', () => ({ de: dataIso(), pana: dataIso(2) }));

  const [grup, setGrup] = useStareSesiune('piete.grup', 'goluri');
  const [cod, setCod] = useStareSesiune<CodPiata>('piete.cod', 'GOLURI_PESTE');
  const [linie, setLinie] = useStareSesiune<number | null>('piete.linie', 2.5);
  /** Limita „sub Y" a piețelor-interval; ignorată la celelalte piețe. */
  const [linieSus, setLinieSus] = useStareSesiune<number | null>('piete.linieSus', null);
  const [prag, setPrag] = useStareSesiune('piete.prag', 30);
  /** Gol = toate campionatele. */
  const [ligiSelectate, setLigiSelectate] = useStareSesiune<number[]>('piete.ligi', []);
  const { favorite, comuta } = useFavoritePiete();

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getPieteZile(3, competitie, perioada, locatie)
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
  }, [incercare, competitie, perioada, locatie]);

  function schimbaCompetitie(valoare: FiltruCompetitie) {
    salveazaFiltruCompetitie(valoare);
    setCompetitie(valoare);
  }

  function schimbaLocatie(valoare: FiltruLocatie) {
    salveazaFiltruLocatie(valoare);
    setLocatie(valoare);
  }

  const sus = intervalValid(grup, linie, linieSus)?.[1] ?? null;
  const ligi = useMemo(
    () => ligiDisponibile(date?.zile ?? [], cod, linie, prag, sus),
    [date, cod, linie, prag, sus],
  );
  // datele reincarcate pot pierde un campionat selectat — nu-l lasam sa filtreze pe nimic
  const selectate = useMemo(() => ligiValide(ligiSelectate, ligi), [ligiSelectate, ligi]);
  const zile = useMemo(
    () => filtreaza(date?.zile ?? [], cod, linie, prag, selectate, sus),
    [date, cod, linie, prag, selectate, sus],
  );
  const total = numaraRanduri(zile);
  const etichetaAleasa = etichetaPiata(grup, cod, linie, sus);

  function schimbaGrup(nou: string) {
    setGrup(nou);
    setCod(codValid(nou, null));
    const interval = intervalValid(nou, linie, linieSus);
    if (interval) {
      setLinie(interval[0]);
      setLinieSus(interval[1]);
    } else {
      setLinie(linieValida(nou, linie));
    }
  }

  function schimbaInterval(jos: number, susNou: number) {
    setLinie(jos);
    setLinieSus(susNou);
  }

  function schimbaPiata(nouCod: CodPiata, nouaLinie: number | null) {
    setCod(nouCod);
    setLinie(nouaLinie);
  }

  return (
    <PageLayout>
      <div className="mb-5">
        <h1 className="text-2xl font-extrabold text-ink">Piețe</h1>
        <p className="mt-1 text-sm text-ink2">
          Cele mai bune șanse din perioada aleasă (maxim {MAX_ZILE} zile).
        </p>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <CalendarPerioada valoare={perioada} onSchimba={setPerioada} maxZile={MAX_ZILE} />
          <ComutatorCompetitie valoare={competitie} onSchimba={schimbaCompetitie} />
          <ComutatorLocatie valoare={locatie} onSchimba={schimbaLocatie} />
        </div>
      </div>

      <Card className="mb-6 p-4 sm:p-5">
        <FiltrePiete
          grup={grup}
          cod={cod}
          linie={linie}
          linieSus={sus}
          prag={prag}
          ligi={ligi}
          ligiSelectate={selectate}
          onGrup={schimbaGrup}
          onPiata={schimbaPiata}
          onPrag={setPrag}
          onLigi={setLigiSelectate}
          onInterval={schimbaInterval}
        />
      </Card>

      {loading && (
        <Card className="space-y-3 p-4">
          {Array.from({ length: 8 }, (_, i) => (
            <div key={i} className="flex items-center gap-3">
              <Skeleton className="h-3.5 w-10 shrink-0" />
              <Skeleton className="h-3.5 flex-1" />
              <Skeleton className="h-3.5 w-12 shrink-0" />
            </div>
          ))}
        </Card>
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

      {!loading && !eroare && total === 0 && (
        <Card>
          <EmptyState
            titlu="Niciun meci peste prag"
            mesaj={
              selectate.length > 0
                ? `Niciun meci din campionatele alese nu atinge ${prag}% pe piața selectată. Coboară pragul sau adaugă campionate.`
                : `Nicio partidă din perioada aleasă nu atinge ${prag}% pe piața aleasă. Coboară pragul sau schimbă piața.`
            }
          />
        </Card>
      )}

      {!loading && !eroare && total > 0 && (
        <>
          <p className="mb-4 text-xs font-semibold text-ink2">
            {total} {total === 1 ? 'meci' : 'meciuri'} peste {prag}%
          </p>
          <div className="space-y-8">
            {zile.map((zi) => (
              <SectiuneZiPiete
                key={zi.data}
                zi={zi}
                piata={etichetaAleasa}
                favorite={favorite}
                onFavorit={comuta}
              />
            ))}
          </div>
        </>
      )}
    </PageLayout>
  );
}
