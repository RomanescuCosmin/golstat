import { useEffect, useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError, getCompetitie } from '../api/client';
import type { JucatorTop, MeciCompetitie, PaginaCompetitie } from '../api/types';
import { ClasamentSnippet } from '../components/echipa/ClasamentSnippet';
import { GrupeCompetitie } from '../components/competitie/GrupeCompetitie';
import { SchemaEliminatorie } from '../components/competitie/SchemaEliminatorie';
import { SelectorSezon } from '../components/echipa/SelectorSezon';
import { PageLayout } from '../components/layout/PageLayout';
import { SelectorLiga } from '../components/meciuri/SelectorLiga';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { LigaLogo } from '../components/ui/LigaLogo';
import { IconBall, IconCalendar, IconFluier, IconTrophy } from '../components/ui/icons';
import { Skeleton, SkeletonCard } from '../components/ui/Skeleton';
import { TeamLogo } from '../components/ui/TeamLogo';
import { numeEchipa } from '../lib/echipa';
import { formatDataScurta, formatOra } from '../lib/format';
import { LIGI_POPULARE, numeLiga } from '../lib/ligi';

/** Card de top jucatori (golgheteri / pasatori) cu lista ordonata si valoarea mare in dreapta. */
function TopCompetitie({ titlu, jucatori }: { titlu: string; jucatori: JucatorTop[] }) {
  return (
    <Card>
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-sm font-extrabold text-ink">{titlu}</h2>
      </div>
      {jucatori.length === 0 ? (
        <EmptyState titlu="Statisticile jucătorilor nu sunt încă disponibile" />
      ) : (
        <ol className="divide-y divide-line">
          {jucatori.map((j, i) => {
            const nume = j.nume ?? 'Jucător necunoscut';
            return (
              <li key={j.playerId ?? `rand-${i}`} className="flex items-center gap-3 px-5 py-3 transition duration-200 hover:bg-bg">
                <span className="w-5 shrink-0 text-center text-xs font-bold tabular-nums text-ink2">{i + 1}</span>
                <TeamLogo nume={nume} logo={j.foto} size={32} className="rounded-full" />
                <span className="flex min-w-0 flex-1 flex-col">
                  {j.playerId != null ? (
                    <Link to={`/jucator/${j.playerId}`} className="truncate text-sm font-semibold text-ink hover:text-primary">
                      {nume}
                    </Link>
                  ) : (
                    <span className="truncate text-sm font-semibold text-ink">{nume}</span>
                  )}
                  <span className="flex items-center gap-1.5 text-xs text-ink2">
                    <TeamLogo nume={j.echipa.nume} logo={j.echipa.logo} size={14} />
                    <span className="truncate">{numeEchipa(j.echipa)}</span>
                  </span>
                </span>
                <span className="shrink-0 text-lg font-extrabold tabular-nums text-primary">{j.valoare}</span>
              </li>
            );
          })}
        </ol>
      )}
    </Card>
  );
}

/** Un rand de meci al competitiei: data, gazde – scor/ora – oaspeti; click → pagina meciului. */
function RandMeci({ meci, catre }: { meci: MeciCompetitie; catre: string }) {
  const navigate = useNavigate();
  const cuScor = (meci.terminat || meci.inDesfasurare) && meci.golGazde != null && meci.golOaspeti != null;

  return (
    <button
      type="button"
      onClick={() => navigate(catre)}
      className="flex w-full items-center gap-2.5 px-4 py-3 text-left transition duration-200 hover:bg-bg focus:bg-bg focus:outline-none"
    >
      <span className="flex w-12 shrink-0 flex-col items-center gap-0.5">
        <span className="whitespace-nowrap text-[10px] font-medium leading-none text-ink2/80">
          {formatDataScurta(meci.kickoff)}
        </span>
        {meci.inDesfasurare && <Badge variant="live">LIVE</Badge>}
      </span>

      <span className="flex min-w-0 flex-1 items-center justify-end gap-2">
        <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.gazde)}</span>
        <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={20} />
      </span>

      <span
        className={`w-14 shrink-0 text-center text-sm tabular-nums ${
          cuScor ? (meci.inDesfasurare ? 'font-bold text-accent' : 'font-bold text-ink') : 'font-medium text-ink2'
        }`}
      >
        {cuScor ? `${meci.golGazde} - ${meci.golOaspeti}` : formatOra(meci.kickoff)}
      </span>

      <span className="flex min-w-0 flex-1 items-center gap-2">
        <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={20} />
        <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.oaspeti)}</span>
      </span>
    </button>
  );
}

/** Card cu o lista de meciuri ale competitiei (rezultate sau program). */
function CardMeciuri({
  titlu,
  meciuri,
  mesajGol,
  ruta,
}: {
  titlu: string;
  meciuri: MeciCompetitie[];
  mesajGol: string;
  ruta: (m: MeciCompetitie) => string;
}) {
  return (
    <Card>
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-sm font-extrabold text-ink">{titlu}</h2>
      </div>
      {meciuri.length === 0 ? (
        <EmptyState titlu={mesajGol} />
      ) : (
        <div className="max-h-[26rem] divide-y divide-line overflow-y-auto">
          {meciuri.map((m) => (
            <RandMeci key={m.fixtureId} meci={m} catre={ruta(m)} />
          ))}
        </div>
      )}
    </Card>
  );
}

type SectiuneId = 'clasament' | 'golgheteri' | 'rezultate' | 'program';

const TABURI_SECTIUNE: { id: SectiuneId; eticheta: string; icon: ReactNode }[] = [
  { id: 'clasament', eticheta: 'Clasament', icon: <IconTrophy width={20} height={20} /> },
  { id: 'golgheteri', eticheta: 'Golgheteri', icon: <IconBall width={20} height={20} /> },
  { id: 'rezultate', eticheta: 'Rezultate', icon: <IconFluier width={20} height={20} /> },
  { id: 'program', eticheta: 'Program', icon: <IconCalendar width={20} height={20} /> },
];

/** Carduri-selector pentru secțiunile competiției: o singură secțiune vizibilă la un moment dat. */
function SelectorSectiune({ activ, onSchimba }: { activ: SectiuneId; onSchimba: (id: SectiuneId) => void }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4" role="tablist">
      {TABURI_SECTIUNE.map((t) => {
        const selectat = t.id === activ;
        return (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={selectat}
            onClick={() => onSchimba(t.id)}
            className={`group flex items-center gap-3 rounded-card border px-4 py-3 text-left transition duration-200 ${
              selectat
                ? 'border-primary/40 bg-primary/[0.04] shadow-card dark:bg-primary/10 dark:shadow-none'
                : 'border-line bg-card hover:border-primary/30 hover:bg-bg'
            }`}
          >
            <span
              className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-[10px] transition duration-200 ${
                selectat
                  ? 'bg-gradient-to-br from-primary to-[#1D4ED8] text-white shadow-[0_4px_12px_rgb(var(--gs-primary)/0.35)]'
                  : 'bg-ink2/10 text-ink2 group-hover:text-ink'
              }`}
            >
              {t.icon}
            </span>
            <span className={`truncate text-sm font-bold ${selectat ? 'text-ink' : 'text-ink2 group-hover:text-ink'}`}>
              {t.eticheta}
            </span>
          </button>
        );
      })}
    </div>
  );
}

/** Pagina unei competiții: antet cu selectoare, apoi secțiuni comutabile (clasament / golgheteri / rezultate / program). */
export function CompetitiePage() {
  const { leagueId } = useParams<{ leagueId: string }>();
  const id = Number(leagueId);
  const navigate = useNavigate();

  const [date, setDate] = useState<PaginaCompetitie | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);
  const [incercare, setIncercare] = useState(0);
  const [sezon, setSezon] = useState<number | null>(null);
  const [sectiune, setSectiune] = useState<SectiuneId>('clasament');

  // schimbarea competitiei reseteaza sezonul selectat
  useEffect(() => {
    setSezon(null);
  }, [id]);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setEroare(new ApiError(0, 'Competiție invalidă', 'Identificatorul competiției nu este valid.'));
      setLoading(false);
      return;
    }
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getCompetitie(id, sezon ?? undefined)
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
  }, [id, incercare, sezon]);

  const optiuniLigi = LIGI_POPULARE.map((ligaId) => ({ id: ligaId, nume: numeLiga(ligaId) }));

  return (
    <PageLayout>
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink2" aria-label="Breadcrumb">
        <Link to="/" className="font-medium hover:text-primary">
          Competiții
        </Link>
        <span aria-hidden>›</span>
        <span className="font-semibold text-ink">{date?.antet.nume ?? numeLiga(id)}</span>
      </nav>

      {loading && (
        <div className="space-y-5">
          <Card className="flex flex-wrap items-center gap-5 p-6">
            <Skeleton className="h-16 w-16 shrink-0 rounded-full" />
            <div className="min-w-0 flex-1 space-y-2">
              <Skeleton className="h-8 w-64 max-w-full" />
              <Skeleton className="h-4 w-32" />
            </div>
            <div className="flex items-center gap-2.5">
              <Skeleton className="h-9 w-28 rounded-btn" />
              <Skeleton className="h-9 w-36 rounded-btn" />
            </div>
          </Card>
          <div className="grid items-start gap-5 lg:grid-cols-3">
            <Card className="min-w-0 lg:col-span-2">
              <div className="border-b border-line px-5 py-4">
                <Skeleton className="h-4 w-28" />
              </div>
              <div className="divide-y divide-line">
                {Array.from({ length: 10 }, (_, i) => (
                  <div key={i} className="flex items-center gap-3 px-5 py-3">
                    <Skeleton className="h-3.5 w-5 shrink-0" />
                    <Skeleton className="h-6 w-6 shrink-0 rounded-full" />
                    <Skeleton className={`h-3.5 ${i % 3 === 0 ? 'w-2/5' : i % 3 === 1 ? 'w-1/3' : 'w-1/2'}`} />
                    <span className="flex-1" />
                    <Skeleton className="h-3.5 w-8 shrink-0" />
                    <Skeleton className="h-3.5 w-8 shrink-0" />
                  </div>
                ))}
              </div>
            </Card>
            <div className="min-w-0 space-y-5">
              <SkeletonCard randuri={5} />
              <SkeletonCard randuri={5} />
            </div>
          </div>
        </div>
      )}

      {!loading && eroare && (
        <Card>
          {eroare.status === 404 ? (
            <ErrorState titlu="Competiția nu a fost găsită" mesaj="Nu există date pentru această competiție." />
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
          <Card className="flex flex-wrap items-center gap-5 p-6">
            <LigaLogo id={date.antet.leagueId} logo={date.antet.logo} nume={date.antet.nume} size={64} />
            <div className="min-w-0 flex-1">
              <h1 className="truncate text-2xl font-extrabold text-ink md:text-3xl">
                {date.antet.nume ?? numeLiga(date.antet.leagueId)}
              </h1>
              {date.antet.tara && <p className="mt-1 text-sm font-medium text-ink2">{date.antet.tara}</p>}
            </div>
            <div className="flex flex-wrap items-center gap-2.5">
              <SelectorSezon
                sezoane={date.antet.sezoane}
                valoare={sezon ?? date.antet.sezon}
                onChange={setSezon}
              />
              <SelectorLiga
                leagueId={date.antet.leagueId}
                optiuni={optiuniLigi}
                onChange={(v) => {
                  if (v != null) navigate(`/competitie/${v}`);
                }}
              />
            </div>
          </Card>

          <SelectorSectiune activ={sectiune} onSchimba={setSectiune} />

          <div key={sectiune} className="animate-fade-in">
            {sectiune === 'clasament' && (
              <div className="space-y-5">
                {date.grupe.length > 0 ? (
                  <GrupeCompetitie grupe={date.grupe} />
                ) : date.clasament.length > 0 ? (
                  <ClasamentSnippet randuri={date.clasament} teamId={-1} />
                ) : (
                  <Card>
                    <EmptyState
                      titlu="Clasament indisponibil"
                      mesaj="Clasamentul acestei competiții nu este încă disponibil."
                    />
                  </Card>
                )}
                {date.eliminatorii.length > 0 && <SchemaEliminatorie faze={date.eliminatorii} />}
              </div>
            )}

            {sectiune === 'golgheteri' && (
              <div className="grid items-start gap-5 lg:grid-cols-2">
                <TopCompetitie titlu="Golgheteri" jucatori={date.golgheteri} />
                <TopCompetitie titlu="Pase decisive" jucatori={date.pasatori} />
              </div>
            )}

            {sectiune === 'rezultate' && (
              <CardMeciuri
                titlu="Rezultate"
                meciuri={date.rezultate}
                mesajGol="Nu există rezultate în acest sezon."
                ruta={(m) => `/meci/${m.fixtureId}/centru`}
              />
            )}

            {sectiune === 'program' && (
              <CardMeciuri
                titlu="Program"
                meciuri={date.urmatoare}
                mesajGol="Nu există meciuri programate."
                ruta={(m) => `/meci/${m.fixtureId}`}
              />
            )}
          </div>
        </div>
      )}
    </PageLayout>
  );
}
