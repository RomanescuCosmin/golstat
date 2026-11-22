import type { EchipaDto, FereastraFormaDto, FormaEchipaDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { RandMeciForma } from './RandMeciForma';

function RandFereastra({ eticheta, fereastra, echipa }: { eticheta: string; fereastra: FereastraFormaDto; echipa: EchipaDto }) {
  return (
    <div>
      <div className="mb-1 flex items-baseline justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wide text-ink2">{eticheta}</span>
        {fereastra.meciuri.length > 0 && (
          <span className="text-[11px] tabular-nums text-ink2" title="Goluri marcate / primite pe meci, pe această fereastră">
            {fereastra.goluriMarcatePeMeci.toFixed(1)} / {fereastra.goluriPrimitePeMeci.toFixed(1)} gol
          </span>
        )}
      </div>
      {fereastra.meciuri.length === 0 ? (
        <span className="text-xs text-ink2">Fără meciuri</span>
      ) : (
        <ul className="divide-y divide-line">
          {fereastra.meciuri.map((meci, i) => (
            <RandMeciForma key={`${meci.data}-${i}`} meci={meci} echipa={echipa} />
          ))}
        </ul>
      )}
    </div>
  );
}

function BlocForma({ echipa, forma, locatie }: { echipa: EchipaDto; forma: FormaEchipaDto; locatie: string }) {
  return (
    <div>
      <div className="mb-2 flex items-center gap-2">
        <TeamLogo nume={echipa.nume} logo={echipa.logo} size={24} />
        <span className="min-w-0 truncate text-sm font-bold text-ink">{numeEchipa(echipa)}</span>
      </div>
      <div className="space-y-2">
        <RandFereastra eticheta={locatie} fereastra={forma.locatie} echipa={echipa} />
        <RandFereastra eticheta="General" fereastra={forma.general} echipa={echipa} />
      </div>
    </div>
  );
}

interface FormaEchipeProps {
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  formaGazde: FormaEchipaDto;
  formaOaspeti: FormaEchipaDto;
}

/**
 * Rezultatele recente: pentru gazde ultimele 7 ACASĂ + 7 generale, pentru oaspeți ultimele 7 în
 * DEPLASARE + 7 generale, fiecare meci ca gazde · scor · oaspeți.
 */
export function FormaEchipe({ gazde, oaspeti, formaGazde, formaOaspeti }: FormaEchipeProps) {
  return (
    <Card className="h-full p-5">
      <h2 className="text-base font-bold text-ink">
        Rezultate <span className="font-normal text-ink2">(ultimele 7)</span>
      </h2>
      <div className="mt-4 space-y-5">
        <BlocForma echipa={gazde} forma={formaGazde} locatie="Acasă" />
        <BlocForma echipa={oaspeti} forma={formaOaspeti} locatie="Deplasare" />
      </div>
    </Card>
  );
}
