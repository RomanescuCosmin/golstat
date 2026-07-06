import type { EchipaDto, FereastraFormaDto, FormaEchipaDto, FormaMeciDto } from '../../api/types';
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

function RandFereastra({ eticheta, fereastra }: { eticheta: string; fereastra: FereastraFormaDto }) {
  return (
    <div className="flex items-center gap-2">
      <span className="w-20 shrink-0 text-[11px] font-semibold uppercase tracking-wide text-ink2">
        {eticheta}
      </span>
      {fereastra.meciuri.length === 0 ? (
        <span className="text-xs text-ink2">Fără meciuri</span>
      ) : (
        <div className="flex flex-wrap gap-1.5">
          {fereastra.meciuri.map((meci, i) => (
            <BadgeRezultat key={`${meci.data}-${i}`} meci={meci} />
          ))}
        </div>
      )}
      {fereastra.meciuri.length > 0 && (
        <span
          className="ml-auto shrink-0 text-[11px] tabular-nums text-ink2"
          title="Goluri marcate / primite pe meci, pe această fereastră"
        >
          {fereastra.goluriMarcatePeMeci.toFixed(1)} / {fereastra.goluriPrimitePeMeci.toFixed(1)} gol
        </span>
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
        <RandFereastra eticheta={locatie} fereastra={forma.locatie} />
        <RandFereastra eticheta="General" fereastra={forma.general} />
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
 * DEPLASARE + 7 generale (badge-uri V/E/I + mediile marcate/primite pe fereastră).
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
