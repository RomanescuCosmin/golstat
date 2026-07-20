import type { CodPiata } from '../../api/types';
import { PIETE, piataDupaGrup } from '../../lib/piete';
import { Taburi, type Tab } from '../ui/Taburi';
import { PragSlider } from './PragSlider';

const TABURI: Tab[] = PIETE.map((p) => ({ id: p.grup, eticheta: p.eticheta }));

/** Pastilă de selecție (linie sau direcție), în stilul toggle-urilor din filtrele de meciuri. */
function Pastila({
  activ,
  onClick,
  children,
}: {
  activ: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-pressed={activ}
      onClick={onClick}
      className={`shrink-0 rounded-full px-3 py-1.5 text-xs font-semibold transition ${
        activ
          ? 'bg-primary text-white shadow-card'
          : 'bg-ink2/10 text-ink2 hover:bg-ink2/20 hover:text-ink dark:bg-ink2/15'
      }`}
    >
      {children}
    </button>
  );
}

interface Props {
  grup: string;
  cod: CodPiata;
  linie: number | null;
  prag: number;
  onGrup: (grup: string) => void;
  onCod: (cod: CodPiata) => void;
  onLinie: (linie: number) => void;
  onPrag: (prag: number) => void;
}

/** Selectorul de piață + linie + direcție + pragul minim. */
export function FiltrePiete({ grup, cod, linie, prag, onGrup, onCod, onLinie, onPrag }: Props) {
  const definitie = piataDupaGrup(grup);
  return (
    <div className="space-y-3">
      <Taburi taburi={TABURI} activ={grup} onSchimba={onGrup} varianta="pilule" />
      <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
        {definitie.optiuni.length > 1 && (
          <div className="flex gap-2" role="group" aria-label="Direcție">
            {definitie.optiuni.map((o) => (
              <Pastila key={o.cod} activ={o.cod === cod} onClick={() => onCod(o.cod)}>
                {o.eticheta}
              </Pastila>
            ))}
          </div>
        )}
        {definitie.linii.length > 0 && (
          <div className="flex gap-2 overflow-x-auto" role="group" aria-label="Linie">
            {definitie.linii.map((l) => (
              <Pastila key={l} activ={l === linie} onClick={() => onLinie(l)}>
                {l}
              </Pastila>
            ))}
          </div>
        )}
        <PragSlider prag={prag} onSchimba={onPrag} />
      </div>
    </div>
  );
}
