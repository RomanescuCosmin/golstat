import type { EchipaDto, FormaMeciDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta } from '../../lib/format';
import { TeamLogo } from '../ui/TeamLogo';

const CULOARE = { V: 'bg-win', E: 'bg-draw', I: 'bg-accent' } as const;

function Partea({ echipa, proprie, dreapta }: { echipa: EchipaDto | null; proprie: boolean; dreapta?: boolean }) {
  return (
    <span className={`flex min-w-0 items-center gap-1.5 ${dreapta ? 'flex-row-reverse text-right' : ''}`}>
      <TeamLogo nume={echipa?.nume} logo={echipa?.logo ?? null} size={20} className="shrink-0" />
      <span className={`min-w-0 truncate ${proprie ? 'font-bold text-ink' : 'text-ink2'}`}>
        {echipa ? numeEchipa(echipa) : 'Necunoscut'}
      </span>
    </span>
  );
}

/**
 * Un meci din forma unei echipe, scris ca pe un tabel de rezultate: gazde · scor · oaspeți (scorul în
 * ordinea gazde-oaspeți, nu „marcate-primite"), echipa urmărită îngroșată, chip V/E/I la final.
 */
export function RandMeciForma({ meci, echipa, cuData = true }: { meci: FormaMeciDto; echipa: EchipaDto; cuData?: boolean }) {
  const adversar = meci.adversar ?? null;
  const golGazde = meci.acasa ? meci.golMarcate : meci.golPrimite;
  const golOaspeti = meci.acasa ? meci.golPrimite : meci.golMarcate;
  return (
    <li className="flex items-center gap-2 py-1.5 text-sm">
      {cuData && <span className="w-11 shrink-0 text-[11px] tabular-nums text-ink2">{formatDataScurta(meci.data)}</span>}
      <span className="grid min-w-0 flex-1 grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2">
        <Partea echipa={meci.acasa ? echipa : adversar} proprie={meci.acasa} dreapta />
        <span className="rounded bg-bg px-1.5 py-0.5 text-center font-bold tabular-nums text-ink">
          {golGazde}-{golOaspeti}
        </span>
        <Partea echipa={meci.acasa ? adversar : echipa} proprie={!meci.acasa} />
      </span>
      <span
        title={meci.acasa ? 'acasă' : 'deplasare'}
        className={`flex h-5 w-5 shrink-0 items-center justify-center rounded text-[11px] font-bold text-white ${CULOARE[meci.rezultat]}`}
      >
        {meci.rezultat}
      </span>
    </li>
  );
}
