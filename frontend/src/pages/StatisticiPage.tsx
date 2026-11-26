import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, getStatisticiLigi } from '../api/client';
import type { StatisticiLiga } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { AntetPagina } from '../components/layout/AntetPagina';
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
function CelulaMedie({
  valoare,
  maxim,
  accent,
  eticheta,
}: {
  valoare: number | null;
  maxim: number;
  accent: boolean;
  eticheta: string;
}) {
  // minimul de 4% face vizibile valorile mici, dar 0 ramane 0 — fara umplere falsa
  const procent =
    valoare != null && valoare > 0 && maxim > 0 ? Math.max(4, Math.round((valoare / maxim) * 100)) : 0;
  return (
    <td data-eticheta={eticheta} className="block text-center align-middle before:mb-1 before:block before:text-[10px] before:font-semibold before:uppercase before:tracking-wide before:text-ink2 before:content-[attr(data-eticheta)] md:before:hidden md:table-cell md:px-4 md:py-3.5">
      <span className="text-sm font-semibold tabular-nums text-ink">{formatMedie(valoare)}</span>
      <span className={`mx-auto mt-1.5 block h-1 w-12 overflow-hidden md:w-16 rounded-full ${accent ? 'bg-accent/15' : 'bg-primary/15'}`}>
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
      <AntetPagina titlu="Statistici" subtitlu="Tendințe pe ligă — medii pe meci." icon={<IconChart width={22} height={22} />} />

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
          <div className="md:overflow-x-auto">
            <table className="block w-full border-collapse text-left md:table md:min-w-[720px]">
              <thead className="hidden md:table-header-group">
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
              <tbody className="block md:table-row-group">
                {randuri.map((liga, index) => {
                  const sezon = etichetaSezon(liga.sezon);
                  return (
                    <tr
                      key={liga.leagueId}
                      onClick={() => navigate(`/competitie/${liga.leagueId}`)}
                      className={`grid cursor-pointer grid-cols-4 gap-x-1 gap-y-3 border-b border-line/60 px-4 py-4 transition-colors last:border-b-0 hover:bg-bg md:table-row md:p-0 ${
                        index === 0 ? 'bg-primary/5' : ''
                      }`}
                    >
                      <td className="col-span-4 block align-middle md:table-cell md:px-5 md:py-3.5">
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
                        <CelulaMedie
                          key={col.cheie}
                          valoare={liga[col.cheie]}
                          maxim={maxime[col.cheie]}
                          accent={col.accent}
                          eticheta={col.eticheta.replace('/meci', '')}
                        />
                      ))}
                      <td className="hidden px-3 py-3.5 text-right align-middle md:table-cell">
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
