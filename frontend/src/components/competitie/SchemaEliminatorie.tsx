import { useNavigate } from 'react-router-dom';
import type { FazaEliminatorie, MeciCompetitie } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { TeamLogo } from '../ui/TeamLogo';
import { formatDataScurta } from '../../lib/format';

/** Traduce eticheta API-Football a rundei in romana; pastreaza originalul daca nu-l cunoastem. */
function numeRunda(runda: string): string {
  const map: Record<string, string> = {
    'round of 32': '16-imi',
    'round of 16': 'Optimi',
    'quarter-finals': 'Sferturi',
    'semi-finals': 'Semifinale',
    'final': 'Finală',
    '3rd place final': 'Finala mică',
  };
  return map[runda.trim().toLowerCase()] ?? runda;
}

/** Un rand (o echipa) din cardul de meci; scorul apare doar la meciurile jucate/live. */
function RandEchipa({
  nume,
  logo,
  scor,
  invingator,
  cuScor,
}: {
  nume: string | null;
  logo: string | null;
  scor: number | null;
  invingator: boolean;
  cuScor: boolean;
}) {
  return (
    <div className={`flex items-center gap-2 ${invingator ? 'text-ink' : 'text-ink2'}`}>
      <TeamLogo nume={nume ?? ''} logo={logo} size={18} />
      <span className={`min-w-0 flex-1 truncate text-sm ${invingator ? 'font-bold' : 'font-medium'}`}>
        {nume ?? 'TBD'}
      </span>
      {cuScor && (
        <span className={`shrink-0 text-sm tabular-nums ${invingator ? 'font-bold text-ink' : 'text-ink2'}`}>
          {scor ?? '–'}
        </span>
      )}
    </div>
  );
}

/** Cardul unui meci eliminatoriu: gazde peste oaspeti, invingatorul ingrosat; click → pagina meciului. */
function CardMeci({ meci }: { meci: MeciCompetitie }) {
  const navigate = useNavigate();
  const cuScor = (meci.terminat || meci.inDesfasurare) && meci.golGazde != null && meci.golOaspeti != null;
  const gazdeInving = cuScor && (meci.golGazde ?? 0) > (meci.golOaspeti ?? 0);
  const oaspetiInving = cuScor && (meci.golOaspeti ?? 0) > (meci.golGazde ?? 0);

  return (
    <button
      type="button"
      onClick={() => navigate(`/meci/${meci.fixtureId}${meci.terminat ? '/centru' : ''}`)}
      className="flex w-full flex-col gap-1.5 rounded-lg border border-line bg-card px-3 py-2.5 text-left transition duration-200 hover:border-primary/40 hover:bg-bg focus:outline-none focus:ring-2 focus:ring-primary/30"
    >
      <RandEchipa
        nume={meci.gazde.nume}
        logo={meci.gazde.logo}
        scor={meci.golGazde}
        invingator={!cuScor || gazdeInving}
        cuScor={cuScor}
      />
      <RandEchipa
        nume={meci.oaspeti.nume}
        logo={meci.oaspeti.logo}
        scor={meci.golOaspeti}
        invingator={!cuScor || oaspetiInving}
        cuScor={cuScor}
      />
      <span className="text-[10px] font-medium text-ink2/70">
        {meci.inDesfasurare ? 'LIVE' : meci.kickoff ? formatDataScurta(meci.kickoff) : 'De programat'}
      </span>
    </button>
  );
}

/**
 * Schema fazelor eliminatorii: o coloana per faza (optimi → sferturi → … → finală), fiecare cu
 * meciurile ei. Deruleaza orizontal pe ecrane mici ca sa pastreze schema completa.
 */
export function SchemaEliminatorie({ faze }: { faze: FazaEliminatorie[] }) {
  return (
    <Card>
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-sm font-extrabold text-ink">Faze eliminatorii</h2>
      </div>
      {faze.length === 0 ? (
        <EmptyState titlu="Fazele eliminatorii nu sunt încă disponibile" />
      ) : (
        <div className="overflow-x-auto">
          <div className="flex min-w-max gap-4 p-4">
            {faze.map((faza) => (
              <div key={faza.runda} className="flex w-52 shrink-0 flex-col gap-3">
                <h3 className="text-center text-xs font-bold uppercase tracking-wide text-ink2">
                  {numeRunda(faza.runda)}
                </h3>
                <div className="flex flex-1 flex-col justify-around gap-3">
                  {faza.meciuri.map((meci) => (
                    <CardMeci key={meci.fixtureId} meci={meci} />
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}
