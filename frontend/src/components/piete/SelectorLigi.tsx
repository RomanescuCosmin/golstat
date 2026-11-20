import type { OptiuneLiga } from '../../lib/piete';
import { LigaLogo } from '../ui/LigaLogo';

interface Props {
  ligi: OptiuneLiga[];
  selectate: number[];
  onSchimba: (selectate: number[]) => void;
}

function Bifa({ activ }: { activ: boolean }) {
  return (
    <span
      aria-hidden="true"
      className={`flex h-4 w-4 shrink-0 items-center justify-center rounded border text-[10px] font-black ${
        activ ? 'border-piata bg-piata text-white' : 'border-line'
      }`}
    >
      {activ ? '✓' : ''}
    </span>
  );
}

/**
 * Selecție multiplă de campionate, în meniul chip-ului „Campionate". Nimic selectat = toate
 * (nu „niciunul") — de aceea „Toate" e bifat exact când selecția e goală și o golește la click.
 *
 * Campionatele fără meciuri la piața/pragul curent rămân în listă, estompate: lista nu sare sub
 * deget în timp ce schimbi pragul.
 */
export function SelectorLigi({ ligi, selectate, onSchimba }: Props) {
  if (ligi.length === 0) {
    return <p className="px-2 py-1 text-sm text-ink2">Niciun campionat în perioada aleasă.</p>;
  }

  const toate = selectate.length === 0;

  function comuta(id: number) {
    onSchimba(selectate.includes(id) ? selectate.filter((x) => x !== id) : [...selectate, id]);
  }

  const rand = 'flex w-full items-center gap-2.5 rounded-lg px-2 py-2 text-left text-sm transition hover:bg-bg';

  return (
    <div className="flex flex-col" role="group" aria-label="Campionate">
      <button type="button" aria-pressed={toate} onClick={() => onSchimba([])} className={`${rand} font-semibold text-ink`}>
        <Bifa activ={toate} />
        Toate
      </button>
      <div className="my-1 border-t border-line" />
      <div className="max-h-72 overflow-y-auto pr-1">
        {ligi.map((liga) => {
          const activ = selectate.includes(liga.id);
          return (
            <button
              key={liga.id}
              type="button"
              aria-pressed={activ}
              onClick={() => comuta(liga.id)}
              title={liga.nume ?? undefined}
              className={`${rand} ${activ ? 'text-ink' : 'text-ink2'} ${liga.numar === 0 && !activ ? 'opacity-50' : ''}`}
            >
              <Bifa activ={activ} />
              <LigaLogo id={liga.id} logo={liga.logo} nume={liga.nume} size={18} />
              <span className="min-w-0 flex-1 truncate font-semibold">{liga.nume ?? `Liga #${liga.id}`}</span>
              <span className="tabular-nums text-xs text-ink2">{liga.numar}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
