import { useEffect, useState } from 'react';
import { ApiError, getProgram } from '../api/client';
import type { Program } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { BandaZileProgram } from '../components/program/BandaZileProgram';
import { SectiuneProgram } from '../components/program/SectiuneProgram';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton, SkeletonRand } from '../components/ui/Skeleton';

export function ProgramPage() {
  const [program, setProgram] = useState<Program | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);
  const [ziSelectata, setZiSelectata] = useState<string | null>(null);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getProgram(7)
      .then((rezultat) => {
        if (!anulat) {
          setProgram(rezultat);
          // Deschidem direct pe ziua cea mai apropiata, ca sa nu curga toate cele 7 zile.
          setZiSelectata(rezultat.zile[0]?.data ?? null);
        }
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setProgram(null);
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

  const zile = program?.zile ?? [];
  const zileAfisate = ziSelectata == null ? zile : zile.filter((z) => z.data === ziSelectata);

  return (
    <PageLayout>
      <div className="mb-5">
        <h1 className="text-2xl font-extrabold text-ink">Program</h1>
        <p className="text-sm text-ink2">
          Meciuri viitoare din toate competițiile urmărite, pe următoarele 7 zile. Alege o zi din bandă.
        </p>
      </div>

      {!loading && !eroare && zile.length > 0 && (
        <div className="mb-5">
          <BandaZileProgram zile={zile} selectata={ziSelectata} onSelect={setZiSelectata} />
        </div>
      )}

      {loading && (
        <div className="space-y-8">
          {[4, 3, 3].map((randuri, i) => (
            <div key={i} className="space-y-3">
              <Skeleton className="h-5 w-48" />
              <Card>
                <div className="divide-y divide-line">
                  {Array.from({ length: randuri }, (_, r) => (
                    <SkeletonRand key={r} />
                  ))}
                </div>
              </Card>
            </div>
          ))}
        </div>
      )}

      {!loading && eroare && (
        <Card>
          <ErrorState titlu={eroare.title} mesaj={eroare.detail ?? eroare.message} onRetry={() => setIncercare((n) => n + 1)} />
        </Card>
      )}

      {!loading && !eroare && zile.length === 0 && (
        <Card>
          <EmptyState titlu="Niciun meci programat" mesaj="Nu există meciuri viitoare colectate în această fereastră." />
        </Card>
      )}

      {!loading && !eroare && zile.length > 0 && zileAfisate.length === 0 && (
        <Card>
          <EmptyState titlu="Niciun meci în ziua selectată" mesaj="Alege altă zi din bandă sau vezi toate zilele." />
        </Card>
      )}

      {!loading && !eroare && zileAfisate.length > 0 && (
        <div className="animate-fade-in space-y-8">
          {zileAfisate.map((zi) => (
            <SectiuneProgram key={zi.data} zi={zi} />
          ))}
        </div>
      )}
    </PageLayout>
  );
}
