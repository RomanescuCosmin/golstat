import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getLive, getMatchCenter } from '../api/client';
import type { MeciCentral, MeciLive } from '../api/types';
import { PageLayout } from '../components/layout/PageLayout';
import { AntetPagina } from '../components/layout/AntetPagina';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { TeamLogo } from '../components/ui/TeamLogo';
import { IconStar } from '../components/ui/icons';
import { useFavorite } from '../hooks/useFavorite';
import { useMeciuriFavorite } from '../hooks/useMeciuriFavorite';
import { numeEchipa } from '../lib/echipa';
import { formatDataScurta, formatOra } from '../lib/format';
import type { MeciFavorit } from '../lib/meciuriFavorite';

const REIMPROSPATARE_MS = 15000;

/** Starea curenta a unui meci favorit: din /live cand e in joc, altfel din detaliul meciului. */
interface Stare {
  golGazde: number | null;
  golOaspeti: number | null;
  status: string | null;
  minut: number | null;
  inDesfasurare: boolean;
  terminat: boolean;
}

function dinLive(m: MeciLive): Stare {
  return {
    golGazde: m.golGazde,
    golOaspeti: m.golOaspeti,
    status: m.status,
    minut: m.minut,
    inDesfasurare: true,
    terminat: false,
  };
}

function dinCentru(m: MeciCentral): Stare {
  return {
    golGazde: m.golGazde,
    golOaspeti: m.golOaspeti,
    status: m.status,
    minut: m.minut,
    inDesfasurare: m.inDesfasurare,
    terminat: m.terminat,
  };
}

/**
 * Pagina Favorite: meciurile marcate cu steluta (notificare + sunet la fiecare gol) si echipele
 * favorite. Scorurile live se reimprospateaza la 15s; restul meciurilor se citesc o data din detaliu.
 */
export function FavoritePage() {
  const favMeciuri = useMeciuriFavorite();
  const favEchipe = useFavorite();
  const [stari, setStari] = useState<Record<number, Stare>>({});

  const chei = favMeciuri.meciuri.map((m) => m.fixtureId).join(',');

  useEffect(() => {
    if (!chei) return;
    let anulat = false;
    const iduri = chei.split(',').map(Number);

    for (const id of iduri) {
      getMatchCenter(id)
        .then((m) => {
          if (!anulat) setStari((s) => ({ ...s, [id]: s[id]?.inDesfasurare ? s[id]! : dinCentru(m) }));
        })
        .catch(() => undefined);
    }

    const live = () =>
      getLive()
        .then((lista) => {
          if (anulat) return;
          setStari((s) => {
            const urm = { ...s };
            for (const m of lista) {
              if (iduri.includes(m.fixtureId)) urm[m.fixtureId] = dinLive(m);
            }
            return urm;
          });
        })
        .catch(() => undefined);
    void live();
    const t = setInterval(() => void live(), REIMPROSPATARE_MS);
    return () => {
      anulat = true;
      clearInterval(t);
    };
  }, [chei]);

  return (
    <PageLayout>
      <AntetPagina
        titlu="Favorite"
        subtitlu="La fiecare gol într-un meci favorit primești notificare cu sunet, pe orice pagină."
        icon={<IconStar width={22} height={22} />}
      />
      <div className="grid items-start gap-4 md:gap-5 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">

        <Card className="p-4 sm:p-5">
          <h2 className="text-base font-bold text-ink">Meciuri favorite</h2>
          {favMeciuri.meciuri.length === 0 ? (
            <EmptyState
              titlu="Niciun meci favorit"
              mesaj="Apasă ⭐ din stânga unui meci, în lista de meciuri, ca să-l urmărești aici."
            />
          ) : (
            <ul className="mt-3 divide-y divide-line">
              {favMeciuri.meciuri.map((m) => (
                <RandFavorit
                  key={m.fixtureId}
                  meci={m}
                  stare={stari[m.fixtureId]}
                  onScoate={() => favMeciuri.comuta(m)}
                />
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-4 sm:p-5">
          <h2 className="text-base font-bold text-ink">Echipe favorite</h2>
          <p className="mt-1 text-xs text-ink2">Primești notificare la golurile din orice meci al acestor echipe.</p>
          {favEchipe.echipe.length === 0 ? (
            <EmptyState titlu="Nicio echipă favorită" mesaj="Apasă ⭐ lângă numele unei echipe." />
          ) : (
            <ul className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {favEchipe.echipe.map((e) => (
                <li key={e.id} className="flex min-h-[48px] items-center gap-2 rounded-xl border border-line bg-bg/40 px-3 py-2">
                  <Link to={`/echipa/${e.id}`} className="flex min-w-0 flex-1 items-center gap-2 hover:text-primary">
                    <TeamLogo nume={e.nume} logo={e.logo} size={22} />
                    <span className="truncate text-sm font-semibold text-ink">{numeEchipa(e)}</span>
                  </Link>
                  <button
                    type="button"
                    onClick={() => favEchipe.comuta(e)}
                    aria-label={`Scoate ${numeEchipa(e)} din favorite`}
                    className="shrink-0 text-primary transition hover:text-accent"
                  >
                    <IconStar width={16} height={16} fill="currentColor" />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </PageLayout>
  );
}

function RandFavorit({ meci, stare, onScoate }: { meci: MeciFavorit; stare?: Stare; onScoate: () => void }) {
  const scor =
    stare && (stare.golGazde != null || stare.golOaspeti != null || stare.inDesfasurare)
      ? `${stare.golGazde ?? 0} – ${stare.golOaspeti ?? 0}`
      : '–';

  return (
    <li className="flex items-center gap-3 py-2.5">
      <button
        type="button"
        onClick={onScoate}
        aria-label="Scoate meciul din favorite"
        className="-m-2 shrink-0 p-2 text-accent transition hover:opacity-70"
      >
        <IconStar width={17} height={17} fill="currentColor" />
      </button>

      <div className="flex w-12 shrink-0 flex-col items-center text-center sm:w-16">
        {stare?.inDesfasurare ? (
          <Badge variant="live">{stare.status === 'HT' ? 'Pauză' : stare.minut != null ? `${stare.minut}'` : 'LIVE'}</Badge>
        ) : stare?.terminat ? (
          <span className="text-[11px] font-semibold uppercase tracking-wide text-ink2">Final</span>
        ) : (
          <>
            <span className="text-sm font-bold tabular-nums text-primary">{formatOra(meci.kickoff)}</span>
            <span className="text-[11px] text-ink2">{formatDataScurta(meci.kickoff)}</span>
          </>
        )}
      </div>

      <Link
        to={`/meci/${meci.fixtureId}/centru`}
        className="grid min-w-0 flex-1 grid-cols-[1fr_auto_1fr] items-center gap-1.5 rounded-xl px-1 py-1 transition hover:bg-bg sm:gap-2 sm:px-2"
      >
        <span className="flex min-w-0 items-center justify-end gap-2">
          <span className="truncate text-right text-sm font-semibold text-ink">{numeEchipa(meci.gazde)}</span>
          <TeamLogo nume={meci.gazde.nume} logo={meci.gazde.logo} size={24} />
        </span>
        <span
          className={`px-2 text-[15px] font-bold tabular-nums ${stare?.inDesfasurare ? 'text-accent' : 'text-ink'}`}
        >
          {scor}
        </span>
        <span className="flex min-w-0 items-center gap-2">
          <TeamLogo nume={meci.oaspeti.nume} logo={meci.oaspeti.logo} size={24} />
          <span className="truncate text-sm font-semibold text-ink">{numeEchipa(meci.oaspeti)}</span>
        </span>
      </Link>

      {meci.ligaNume && <span className="hidden w-36 truncate text-xs text-ink2 md:block">{meci.ligaNume}</span>}
    </li>
  );
}
