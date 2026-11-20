import { PIETE } from '../../lib/piete';

/**
 * Tipurile de piață, în meniul chip-ului „Tip": o grilă pe două coloane, fără scroll orizontal.
 * Accentul e violetul paginii, nu albastrul de navigație, ca selecția să nu semene cu un link.
 */
export function SegmenteTipPiata({
  grup,
  onSchimba,
}: {
  grup: string;
  onSchimba: (grup: string) => void;
}) {
  return (
    <div className="grid grid-cols-2 gap-1" role="tablist" aria-label="Tip piață">
      {PIETE.map((piata) => {
        const selectat = piata.grup === grup;
        return (
          <button
            key={piata.grup}
            type="button"
            role="tab"
            aria-selected={selectat}
            onClick={() => onSchimba(piata.grup)}
            className={`flex items-center gap-2 rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
              selectat ? 'bg-piata text-white' : 'text-ink2 hover:bg-bg hover:text-ink'
            }`}
          >
            <span aria-hidden="true" className="w-5 shrink-0 text-center">{piata.icona}</span>
            <span className="leading-tight">{piata.eticheta}</span>
          </button>
        );
      })}
    </div>
  );
}
