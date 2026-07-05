import { useEffect, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getJucator } from '../api/client';
import type { PaginaJucator, SezonJucator } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { LigaLogo } from '../components/ui/LigaLogo';
import { Skeleton } from '../components/ui/Skeleton';
import { TeamLogo } from '../components/ui/TeamLogo';
import { IconBall, IconCartonas, IconUser } from '../components/ui/icons';

/** Fotografia jucatorului cu fallback pe silueta cand URL-ul extern pica. */
function FotoJucator({ foto, nume }: { foto: string | null; nume: string | null }) {
  const [failed, setFailed] = useState(false);

  if (!foto || failed) {
    return (
      <span className="flex h-24 w-24 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary dark:bg-primary/20">
        <IconUser width={44} height={44} />
      </span>
    );
  }

  return (
    <img
      src={foto}
      alt={nume ?? 'Jucător'}
      width={96}
      height={96}
      onError={() => setFailed(true)}
      className="h-24 w-24 shrink-0 rounded-full border border-line object-cover"
    />
  );
}

function formatSezon(sezon: number | null): string {
  if (sezon == null) return '—';
  return `${sezon}/${String((sezon + 1) % 100).padStart(2, '0')}`;
}

function clasaRating(rating: number): string {
  if (rating >= 7) return 'text-win';
  if (rating < 6) return 'text-accent';
  return 'text-ink';
}

function suma(sezoane: SezonJucator[], cheie: 'aparitii' | 'goluri' | 'pase' | 'galbene'): number {
  return sezoane.reduce((total, s) => total + (s[cheie] ?? 0), 0);
}

function TileSumar({ eticheta, valoare, icon }: { eticheta: string; valoare: number; icon: ReactNode }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-line bg-bg px-4 py-3 dark:bg-card">
      {icon}
      <div className="min-w-0">
        <p className="text-lg font-extrabold tabular-nums text-ink">{valoare}</p>
        <p className="truncate text-xs font-medium text-ink2">{eticheta}</p>
      </div>
    </div>
  );
}

export function JucatorPage() {
  const { playerId } = useParams<{ playerId: string }>();
  const id = Number(playerId);

  const [date, setDate] = useState<PaginaJucator | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setEroare(new ApiError(0, 'Jucător invalid', 'Identificatorul jucătorului nu este valid.'));
      setLoading(false);
      return;
    }
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getJucator(id)
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

  const detalii = date
    ? [date.pozitie, date.nationalitate, date.varsta != null ? `${date.varsta} ani` : null].filter(Boolean)
    : [];

  return (
    <PageLayout>
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink2" aria-label="Breadcrumb">
        <span className="font-medium">Jucători</span>
        <span aria-hidden>›</span>
        <span className="font-semibold text-ink">{date?.nume ?? 'Jucător'}</span>
      </nav>

      {loading && (
        <div className="space-y-5">
          <Card className="p-6">
            <div className="flex flex-wrap items-center gap-5">
              <Skeleton className="h-24 w-24 shrink-0 rounded-full" />
              <div className="min-w-0 flex-1 space-y-2.5">
                <Skeleton className="h-8 w-52 max-w-full" />
                <Skeleton className="h-4 w-72 max-w-full" />
              </div>
            </div>
          </Card>
          <Card className="p-5 sm:p-6">
            <Skeleton className="h-4 w-44" />
            <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
              {Array.from({ length: 4 }, (_, i) => (
                <Skeleton key={i} className="h-16 rounded-xl" />
              ))}
            </div>
            <div className="mt-5 space-y-3">
              <Skeleton className="h-3.5 w-full" />
              {Array.from({ length: 5 }, (_, i) => (
                <Skeleton key={i} className={`h-4 ${i % 2 === 0 ? 'w-full' : 'w-11/12'}`} />
              ))}
            </div>
          </Card>
        </div>
      )}

      {!loading && eroare && (
        <Card>
          {eroare.status === 404 ? (
            <ErrorState titlu="Jucătorul nu a fost găsit" mesaj="Nu există date pentru acest jucător." />
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
        <div className="animate-fade-in space-y-5">
          <Card className="p-6">
            <div className="flex flex-wrap items-center gap-5">
              <FotoJucator foto={date.foto} nume={date.nume} />
              <div className="min-w-0">
                <h1 className="text-2xl font-extrabold text-ink sm:text-3xl">{date.nume ?? 'Jucător'}</h1>
                <div className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-ink2">
                  {detalii.map((d, i) => (
                    <span key={`${i}-${d}`} className="flex items-center gap-2">
                      {i > 0 && <span aria-hidden>·</span>}
                      <span>{d}</span>
                    </span>
                  ))}
                  {date.echipaCurenta && (
                    <span className="flex items-center gap-2">
                      {detalii.length > 0 && <span aria-hidden>·</span>}
                      <Link
                        to={`/echipa/${date.echipaCurenta.id}`}
                        className="flex items-center gap-1.5 font-semibold text-ink hover:text-primary"
                      >
                        <TeamLogo nume={date.echipaCurenta.nume} logo={date.echipaCurenta.logo} size={20} />
                        {date.echipaCurenta.nume}
                      </Link>
                    </span>
                  )}
                </div>
              </div>
            </div>
          </Card>

          <Card className="p-5 sm:p-6">
            <h2 className="text-sm font-extrabold text-ink">Statistici pe sezoane</h2>

            {date.sezoane.length === 0 ? (
              <EmptyState titlu="Nu există încă statistici pentru acest jucător." />
            ) : (
              <>
                <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <TileSumar
                    eticheta="Apariții"
                    valoare={suma(date.sezoane, 'aparitii')}
                    icon={<IconUser width={22} height={22} className="shrink-0 text-primary" />}
                  />
                  <TileSumar
                    eticheta="Goluri"
                    valoare={suma(date.sezoane, 'goluri')}
                    icon={<IconBall width={22} height={22} className="shrink-0 text-ink" />}
                  />
                  <TileSumar
                    eticheta="Pase decisive"
                    valoare={suma(date.sezoane, 'pase')}
                    icon={<IconBall width={22} height={22} className="shrink-0 text-primary" />}
                  />
                  <TileSumar
                    eticheta="Cartonașe galbene"
                    valoare={suma(date.sezoane, 'galbene')}
                    icon={<IconCartonas width={22} height={22} className="shrink-0 text-yellow-400" />}
                  />
                </div>

                <div className="mt-4 overflow-x-auto">
                  <table className="w-full min-w-[760px] text-sm">
                    <thead>
                      <tr className="border-b border-line text-left text-xs font-semibold uppercase tracking-wide text-ink2">
                        <th className="py-2.5 pr-3 font-semibold">Sezon</th>
                        <th className="py-2.5 pr-3 font-semibold">Competiție</th>
                        <th className="py-2.5 pr-3 font-semibold">Echipă</th>
                        <th className="py-2.5 pr-3 text-right font-semibold">Apariții</th>
                        <th className="py-2.5 pr-3 text-right font-semibold">Minute</th>
                        <th className="py-2.5 pr-3 text-right font-semibold">Goluri</th>
                        <th className="py-2.5 pr-3 text-right font-semibold">Pase</th>
                        <th className="py-2.5 pr-3 text-right font-semibold">Galbene</th>
                        <th className="py-2.5 pr-3 text-right font-semibold">Roșii</th>
                        <th className="py-2.5 text-right font-semibold">Rating</th>
                      </tr>
                    </thead>
                    <tbody className="tabular-nums">
                      {date.sezoane.map((s, i) => (
                        <tr key={`${s.leagueId}-${s.sezon}-${s.echipa?.id}-${i}`} className="border-b border-line/60 last:border-0">
                          <td className="whitespace-nowrap py-2.5 pr-3 font-semibold text-ink">{formatSezon(s.sezon)}</td>
                          <td className="py-2.5 pr-3">
                            <span className="flex items-center gap-2 text-ink">
                              <LigaLogo id={s.leagueId} logo={s.ligaLogo} nume={s.liga} size={18} />
                              <span className="whitespace-nowrap">{s.liga ?? '—'}</span>
                            </span>
                          </td>
                          <td className="py-2.5 pr-3">
                            {s.echipa ? (
                              <Link
                                to={`/echipa/${s.echipa.id}`}
                                className="flex items-center gap-2 font-medium text-ink hover:text-primary"
                              >
                                <TeamLogo nume={s.echipa.nume} logo={s.echipa.logo} size={18} />
                                <span className="whitespace-nowrap">{s.echipa.nume}</span>
                              </Link>
                            ) : (
                              <span className="text-ink2">—</span>
                            )}
                          </td>
                          <td className="py-2.5 pr-3 text-right text-ink">{s.aparitii ?? '—'}</td>
                          <td className="py-2.5 pr-3 text-right text-ink">{s.minute ?? '—'}</td>
                          <td className="py-2.5 pr-3 text-right font-semibold text-ink">{s.goluri ?? '—'}</td>
                          <td className="py-2.5 pr-3 text-right text-ink">{s.pase ?? '—'}</td>
                          <td className="py-2.5 pr-3 text-right text-ink">{s.galbene ?? '—'}</td>
                          <td className="py-2.5 pr-3 text-right text-ink">{s.rosii ?? '—'}</td>
                          <td className="py-2.5 text-right">
                            {s.rating != null ? (
                              <span className={`font-bold ${clasaRating(s.rating)}`}>{s.rating.toFixed(1)}</span>
                            ) : (
                              <span className="text-ink2">—</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </Card>
        </div>
      )}
    </PageLayout>
  );
}
