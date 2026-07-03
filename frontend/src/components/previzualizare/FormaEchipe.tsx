import type { EchipaDto, FormaEchipaDto, FormaMeciDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta } from '../../lib/format';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';

function BadgeRezultat({ meci }: { meci: FormaMeciDto }) {
  const clasa = meci.rezultat === 'V' ? 'bg-win' : meci.rezultat === 'E' ? 'bg-draw' : 'bg-accent';
  return (
    <span
      title={`${formatDataScurta(meci.data)} · ${meci.golMarcate}-${meci.golPrimite} (${meci.acasa ? 'acasă' : 'deplasare'})`}
      className={`flex h-6 w-6 items-center justify-center rounded text-xs font-bold text-white ${clasa}`}
    >
      {meci.rezultat}
    </span>
  );
}

function RandForma({ echipa, forma }: { echipa: EchipaDto; forma: FormaEchipaDto }) {
  const ultimele = forma.meciuri.slice(0, 5);
  return (
    <div className="flex items-center gap-3">
      <TeamLogo nume={echipa.nume} logo={echipa.logo} size={28} />
      <span className="min-w-0 flex-1 truncate text-sm font-semibold text-ink">{numeEchipa(echipa)}</span>
      {ultimele.length === 0 ? (
        <span className="text-xs text-ink2">Fără meciuri recente</span>
      ) : (
        <div className="flex gap-1.5">
          {ultimele.map((meci, i) => (
            <BadgeRezultat key={`${meci.data}-${i}`} meci={meci} />
          ))}
        </div>
      )}
    </div>
  );
}

interface FormaEchipeProps {
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  formaGazde: FormaEchipaDto;
  formaOaspeti: FormaEchipaDto;
}

/** Badge-urile V/E/I ale ultimelor 5 meciuri, cate un rand pe echipa. */
export function FormaEchipe({ gazde, oaspeti, formaGazde, formaOaspeti }: FormaEchipeProps) {
  return (
    <Card className="h-full p-5">
      <h2 className="text-base font-bold text-ink">
        Formă echipe <span className="font-normal text-ink2">(ultimele 5)</span>
      </h2>
      <div className="mt-4 space-y-4">
        <RandForma echipa={gazde} forma={formaGazde} />
        <RandForma echipa={oaspeti} forma={formaOaspeti} />
      </div>
    </Card>
  );
}
