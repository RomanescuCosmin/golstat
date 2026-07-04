import { useEffect, useState } from 'react';
import { ApiError, getProgram } from '../api/client';
import type { Program } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { SectiuneProgram } from '../components/program/SectiuneProgram';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';

export function ProgramPage() {
  const [program, setProgram] = useState<Program | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getProgram(7)
      .then((rezultat) => {
        if (!anulat) setProgram(rezultat);
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

  return (
    <PageLayout>
      <div className="mb-5">
        <h1 className="text-2xl font-extrabold text-ink">Program</h1>
        <p className="text-sm text-ink2">Meciuri viitoare din toate competițiile urmărite, pe următoarele 7 zile.</p>
      </div>

      {loading && (
        <div className="flex justify-center py-20">
          <Spinner size={36} />
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

      {!loading && !eroare && zile.length > 0 && (
        <div className="space-y-8">
          {zile.map((zi) => (
            <SectiuneProgram key={zi.data} zi={zi} />
          ))}
        </div>
      )}
    </PageLayout>
  );
}
