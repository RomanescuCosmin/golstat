import { useEffect, useMemo, useState } from 'react';
import { ApiError, getPieteZile } from '../api/client';
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
  ligiDisponibile,
  ligiValide,
  linieValida,
  numaraRanduri,
} from '../lib/piete';

const ZILE = 3;

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

  const [grup, setGrup] = useState('goluri');
  const [cod, setCod] = useState<CodPiata>('GOLURI_PESTE');
  const [linie, setLinie] = useState<number | null>(2.5);
  const [prag, setPrag] = useState(30);
  /** Gol = toate campionatele. */
  const [ligiSelectate, setLigiSelectate] = useState<number[]>([]);
  const { favorite, comuta } = useFavoritePiete();

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getPieteZile(ZILE)
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
  }, [incercare]);

  const ligi = useMemo(
    () => ligiDisponibile(date?.zile ?? [], cod, linie, prag),
    [date, cod, linie, prag],
  );
  // datele reincarcate pot pierde un campionat selectat — nu-l lasam sa filtreze pe nimic
  const selectate = useMemo(() => ligiValide(ligiSelectate, ligi), [ligiSelectate, ligi]);
  const zile = useMemo(
    () => filtreaza(date?.zile ?? [], cod, linie, prag, selectate),
    [date, cod, linie, prag, selectate],
  );
  const total = numaraRanduri(zile);
  const etichetaAleasa = etichetaPiata(grup, cod, linie);

  function schimbaGrup(nou: string) {
    setGrup(nou);
    setCod(codValid(nou, null));
    setLinie(linieValida(nou, linie));
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
          Cele mai bune șanse din următoarele {ZILE} zile.
        </p>
      </div>

      <Card className="mb-6 p-4 sm:p-5">
        <FiltrePiete
          grup={grup}
          cod={cod}
          linie={linie}
          prag={prag}
          ligi={ligi}
          ligiSelectate={selectate}
          onGrup={schimbaGrup}
          onPiata={schimbaPiata}
          onPrag={setPrag}
          onLigi={setLigiSelectate}
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
                : `Nicio partidă din următoarele ${ZILE} zile nu atinge ${prag}% pe piața aleasă. Coboară pragul sau schimbă piața.`
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
