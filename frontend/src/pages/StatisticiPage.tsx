import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, getStatisticiLigi } from '../api/client';
import type { StatisticiLiga } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { IconChart, IconChevronRight } from '../components/ui/icons';
import { LigaLogo } from '../components/ui/LigaLogo';
import { Skeleton } from '../components/ui/Skeleton';

const COLOANE = [
  { cheie: 'medieGoluri', eticheta: 'Goluri/meci', accent: false },
  { cheie: 'medieCornere', eticheta: 'Cornere/meci', accent: false },
  { cheie: 'medieFaulturi', eticheta: 'Faulturi/meci', accent: false },
  { cheie: 'medieCartonase', eticheta: 'Cartonașe/meci', accent: true },
] as const;

type CheieMedie = (typeof COLOANE)[number]['cheie'];

function formatMedie(valoare: number | null): string {
  if (valoare == null) return '–';
  return valoare.toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function etichetaSezon(sezon: number | null): string | null {
  if (sezon == null) return null;
  return `${sezon}/${String((sezon + 1) % 100).padStart(2, '0')}`;
}

/** Celulă numerică cu bară proporțională față de maximul coloanei — comparație vizuală rapidă. */
function CelulaMedie({ valoare, maxim, accent }: { valoare: number | null; maxim: number; accent: boolean }) {
  // minimul de 4% face vizibile valorile mici, dar 0 ramane 0 — fara umplere falsa
  const procent =
    valoare != null && valoare > 0 && maxim > 0 ? Math.max(4, Math.round((valoare / maxim) * 100)) : 0;
  return (
    <td className="px-4 py-3.5 text-center align-middle">
      <span className="text-sm font-semibold tabular-nums text-ink">{formatMedie(valoare)}</span>
      <span className={`mx-auto mt-1.5 block h-1 w-16 overflow-hidden rounded-full ${accent ? 'bg-accent/15' : 'bg-primary/15'}`}>
        {valoare != null && (
          <span className={`block h-full rounded-full ${accent ? 'bg-accent' : 'bg-primary'}`} style={{ width: `${procent}%` }} />
        )}
      </span>
    </td>
  );
}

export function StatisticiPage() {
  const [ligi, setLigi] = useState<StatisticiLiga[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getStatisticiLigi()
      .then((rezultat) => {
        if (!anulat) setLigi(rezultat);
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setLigi(null);
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

  const randuri = ligi ?? [];

  const maxime = useMemo(() => {
    const rezultat = {} as Record<CheieMedie, number>;
    for (const col of COLOANE) {
      rezultat[col.cheie] = randuri.reduce((max, liga) => Math.max(max, liga[col.cheie] ?? 0), 0);
    }
    return rezultat;
  }, [randuri]);

  return (
    <PageLayout>
      <div className="mb-5 flex items-start gap-3">
        <span className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-primary/10 text-primary">
          <IconChart width={20} height={20} />
        </span>
        <div>
          <h1 className="text-2xl font-extrabold text-ink">Statistici</h1>
          <p className="text-sm text-ink2">Tendințe pe ligă — medii pe meci.</p>
        </div>
      </div>

      {loading && (
        <Card>
          <div className="border-b border-line px-5 py-3.5">
            <Skeleton className="h-3.5 w-24" />
          </div>
          <div className="divide-y divide-line/60">
            {Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="flex items-center gap-4 px-5 py-4">
                <Skeleton className="h-6 w-6 shrink-0 rounded-full" />
                <Skeleton className={`h-3.5 shrink-0 ${i % 2 === 0 ? 'w-36' : 'w-28'}`} />
                <div className="flex flex-1 items-center justify-evenly gap-4">
                  {Array.from({ length: 4 }, (_, c) => (
                    <Skeleton key={c} className="h-3.5 w-12" />
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {!loading && eroare && (
        <Card>
          <ErrorState titlu={eroare.title} mesaj={eroare.detail ?? eroare.message} onRetry={() => setIncercare((n) => n + 1)} />
        </Card>
      )}

      {!loading && !eroare && randuri.length === 0 && (
        <Card>
          <EmptyState titlu="Nu există încă statistici pe ligă" mesaj="Mediile pe meci apar după colectarea statisticilor de meci." />
        </Card>
      )}

      {!loading && !eroare && randuri.length > 0 && (
        <Card className="animate-fade-in">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] border-collapse text-left">
              <thead>
                <tr className="border-b border-line text-[11px] font-semibold uppercase tracking-wide text-ink2">
                  <th className="px-5 py-3.5 font-semibold">Competiție</th>
                  {COLOANE.map((col) => (
                    <th key={col.cheie} className="px-4 py-3.5 text-center font-semibold">
                      {col.eticheta}
                    </th>
                  ))}
                  <th className="w-10 px-3 py-3.5" aria-hidden />
                </tr>
              </thead>
              <tbody>
                {randuri.map((liga, index) => {
                  const sezon = etichetaSezon(liga.sezon);
                  return (
                    <tr
                      key={liga.leagueId}
                      onClick={() => navigate(`/competitie/${liga.leagueId}`)}
                      className={`cursor-pointer border-b border-line/60 transition-colors last:border-b-0 hover:bg-bg ${
                        index === 0 ? 'bg-primary/5' : ''
                      }`}
                    >
                      <td className="px-5 py-3.5 align-middle">
                        <div className="flex items-center gap-3">
                          <LigaLogo id={liga.leagueId} logo={liga.logo} nume={liga.nume} size={22} />
                          <div className="min-w-0">
                            <div className="flex items-baseline gap-2">
                              <Link
                                to={`/competitie/${liga.leagueId}`}
                                onClick={(e) => e.stopPropagation()}
                                className="truncate text-sm font-semibold text-ink hover:text-primary"
                              >
                                {liga.nume ?? `Liga #${liga.leagueId}`}
                              </Link>
                              {sezon && <span className="shrink-0 text-[11px] font-medium tabular-nums text-ink2">{sezon}</span>}
                            </div>
                            {liga.tara && <div className="truncate text-xs text-ink2">{liga.tara}</div>}
                          </div>
                        </div>
                      </td>
                      {COLOANE.map((col) => (
                        <CelulaMedie key={col.cheie} valoare={liga[col.cheie]} maxim={maxime[col.cheie]} accent={col.accent} />
                      ))}
                      <td className="px-3 py-3.5 text-right align-middle">
                        <IconChevronRight width={16} height={16} className="ml-auto text-ink2" />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </PageLayout>
  );
}
