import { PIETE } from '../../lib/piete';

/**
 * Tipul de piață, ca un control segmentat: toate opțiunile stau într-un singur grup, cu una
 * singură evidențiată. Înlocuiește pastilele separate — la șase tipuri, un rând de pastile
 * identice nu arăta care e alegerea și care sunt alternativele.
 *
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
    <div className="inline-flex gap-1 rounded-input border border-line bg-card p-1" role="tablist">
      {PIETE.map((piata) => {
        const selectat = piata.grup === grup;
        return (
          <button
            key={piata.grup}
            type="button"
            role="tab"
            aria-selected={selectat}
            onClick={() => onSchimba(piata.grup)}
            className={`flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-btn px-3.5 py-2 text-sm font-semibold transition duration-200 ${
              selectat ? 'bg-piata text-white shadow-card' : 'text-ink2 hover:bg-bg hover:text-ink'
            }`}
          >
            <span aria-hidden="true">{piata.icona}</span>
            {piata.eticheta}
          </button>
        );
      })}
    </div>
  );
}
