import { useState } from 'react';
import type { OptiuneLiga } from '../../lib/piete';
import { LigaLogo } from '../ui/LigaLogo';

interface Props {
  ligi: OptiuneLiga[];
  selectate: number[];
  onSchimba: (selectate: number[]) => void;
}

/** Câte campionate arătăm înainte de „Mai multe" — un rând, pe majoritatea ecranelor. */
const VIZIBILE = 6;

const CLASE_CHIP =
  'flex shrink-0 items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold transition';

/**
 * Selecție multiplă de campionate. Nimic selectat = toate (nu „niciunul") — de aceea pastila
 * „Toate" e activă exact când selecția e goală, și golește selecția la click.
 *
 * Campionatele fără meciuri la piața/pragul curent rămân vizibile, dar estompate: lista nu trebuie
 * să sară sub degetul utilizatorului în timp ce trage slider-ul. Cele selectate rămân mereu
 * afișate, chiar dacă ar cădea dincolo de limita de mai sus.
 */
export function SelectorLigi({ ligi, selectate, onSchimba }: Props) {
  const [extins, setExtins] = useState(false);

  if (ligi.length === 0) return null;

  const toate = selectate.length === 0;
  const aleseSauPrimele = ligi.filter((l, i) => i < VIZIBILE || selectate.includes(l.id));
  const afisate = extins ? ligi : aleseSauPrimele;
  const ascunse = ligi.length - afisate.length;

  function comuta(id: number) {
    onSchimba(selectate.includes(id) ? selectate.filter((x) => x !== id) : [...selectate, id]);
  }

  return (
    <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Campionate">
      <button
        type="button"
        aria-pressed={toate}
        onClick={() => onSchimba([])}
        className={`${CLASE_CHIP} ${
          toate
            ? 'border-piata bg-piata/10 text-piata'
            : 'border-line bg-card text-ink2 hover:border-ink2/40 hover:text-ink'
        }`}
      >
        Toate
      </button>

      {afisate.map((liga) => {
        const activ = selectate.includes(liga.id);
        return (
          <button
            key={liga.id}
            type="button"
            aria-pressed={activ}
            onClick={() => comuta(liga.id)}
            title={liga.nume ?? undefined}
            className={`${CLASE_CHIP} ${
              activ
                ? 'border-piata bg-piata/10 text-piata'
                : 'border-line bg-card text-ink2 hover:border-ink2/40 hover:text-ink'
            } ${liga.numar === 0 && !activ ? 'opacity-50' : ''}`}
          >
            <LigaLogo id={liga.id} logo={liga.logo} nume={liga.nume} size={16} />
            <span className="max-w-[10rem] truncate">{liga.nume ?? `Liga #${liga.id}`}</span>
            <span className={`tabular-nums ${activ ? 'text-piata/70' : 'text-ink2/70'}`}>
              {liga.numar}
            </span>
          </button>
        );
      })}

      {ascunse > 0 && (
        <button
          type="button"
          onClick={() => setExtins(true)}
          className={`${CLASE_CHIP} border-line bg-card text-ink2 hover:border-ink2/40 hover:text-ink`}
        >
          Mai multe
          <span className="tabular-nums text-ink2/70">+{ascunse}</span>
        </button>
      )}
    </div>
  );
}
