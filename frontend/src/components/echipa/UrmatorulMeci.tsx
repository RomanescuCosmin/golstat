import { Link } from 'react-router-dom';
import type { MeciScurt } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatOra } from '../../lib/format';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { LigaLogo } from '../ui/LigaLogo';
import { IconChevronRight } from '../ui/icons';

interface UrmatorulMeciProps {
  meci: MeciScurt;
  /** Echipa paginii (MeciScurt are doar adversarul). */
  echipa: { nume: string | null; logo: string | null };
}

const ziSiData = new Intl.DateTimeFormat('ro-RO', { weekday: 'short', day: 'numeric', month: 'long' });

/** "Sâm, 18 mai" dintr-un ISO cu ora. */
function formatZiData(iso: string): string {
  const text = ziSiData.format(new Date(iso)).replace('.', '');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "Regular Season - 36" → "Etapa 36"; altfel textul brut al rundei. */
function etapa(runda: string | null): string | null {
  if (!runda) return null;
  const nr = /(\d+)\s*$/.exec(runda);
  return nr ? `Etapa ${nr[1]}` : runda;
}

/** "Următorul meci": competitia sus, echipele fata in fata cu data si ora la mijloc, link spre meci. */
export function UrmatorulMeci({ meci, echipa }: UrmatorulMeciProps) {
  const noi = { nume: echipa.nume ?? 'Echipa', logo: echipa.logo };
  const adversar = { nume: numeEchipa(meci.adversar), logo: meci.adversar.logo };
  const gazde = meci.acasa ? noi : adversar;
  const oaspeti = meci.acasa ? adversar : noi;
  const runda = etapa(meci.runda);

  return (
    <Card className="p-5 transition duration-200 hover:shadow-cardHover">
      <h2 className="text-sm font-extrabold text-ink">Următorul meci</h2>

      <div className="mt-4 flex flex-col items-center gap-0.5 text-center">
        <span className="flex items-center gap-2">
          <LigaLogo logo={meci.ligaLogo} nume={meci.liga} size={18} className="text-ink2" />
          <span className="text-sm font-bold text-ink">{meci.liga ?? 'Competiție'}</span>
        </span>
        {runda && <span className="text-xs font-medium text-ink2">{runda}</span>}
      </div>

      <div className="mt-5 grid grid-cols-3 items-center gap-2">
        <Echipa nume={gazde.nume} logo={gazde.logo} />
        <div className="flex flex-col items-center gap-1 text-center">
          <span className="text-sm font-semibold text-ink2">{formatZiData(meci.kickoff)}</span>
          <span className="text-2xl font-extrabold tracking-tight text-primary">{formatOra(meci.kickoff)}</span>
        </div>
        <Echipa nume={oaspeti.nume} logo={oaspeti.logo} />
      </div>

      <div className="mt-5 border-t border-line pt-3.5">
        <Link
          to={`/meci/${meci.fixtureId}`}
          className="flex items-center justify-center gap-1.5 text-sm font-bold text-primary transition duration-200 hover:opacity-70"
        >
          Vezi detalii meci
          <IconChevronRight width={16} height={16} strokeWidth={2.4} />
        </Link>
      </div>
    </Card>
  );
}

function Echipa({ nume, logo }: { nume: string; logo: string | null }) {
  return (
    <div className="flex min-w-0 flex-col items-center gap-2.5 text-center">
      <TeamLogo nume={nume} logo={logo} size={56} />
      <p className="w-full truncate text-sm font-bold text-ink">{nume}</p>
    </div>
  );
}
