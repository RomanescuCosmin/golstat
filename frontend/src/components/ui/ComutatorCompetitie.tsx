import type { FiltruCompetitie } from '../../api/client';

/** „Toate competițiile" / „Doar competiția meciului": ce meciuri intră în ultimele 7 ale echipelor. */
export function ComutatorCompetitie({
  valoare,
  onSchimba,
}: {
  valoare: FiltruCompetitie;
  onSchimba: (valoare: FiltruCompetitie) => void;
}) {
  const optiune = (v: FiltruCompetitie, eticheta: string) => (
    <button
      type="button"
      onClick={() => onSchimba(v)}
      aria-pressed={valoare === v}
      className={`rounded-md px-3 py-1 text-xs font-semibold transition ${
        valoare === v ? 'bg-primary text-white' : 'text-ink2 hover:text-ink'
      }`}
    >
      {eticheta}
    </button>
  );
  return (
    <div className="inline-flex rounded-lg border border-line p-0.5" role="group" aria-label="Meciuri luate în calcul">
      {optiune('toate', 'Toate competițiile')}
      {optiune('curenta', 'Doar competiția meciului')}
    </div>
  );
}
