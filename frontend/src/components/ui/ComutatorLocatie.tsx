import type { FiltruLocatie } from '../../api/client';

/**
 * „Acasă / Deplasare" (regula implicită: gazdele pe ultimele 7 acasă, oaspeții pe ultimele 7 în
 * deplasare) sau „Toate meciurile" (ultimele 7 ale fiecărei echipe, indiferent de teren).
 */
export function ComutatorLocatie({
  valoare,
  onSchimba,
}: {
  valoare: FiltruLocatie;
  onSchimba: (valoare: FiltruLocatie) => void;
}) {
  const optiune = (v: FiltruLocatie, eticheta: string) => (
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
    <div className="inline-flex rounded-lg border border-line p-0.5" role="group" aria-label="Teren">
      {optiune('separat', 'Acasă / Deplasare')}
      {optiune('toate', 'Toate meciurile')}
    </div>
  );
}
