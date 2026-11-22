import type { CodPiata } from '../../api/types';
import { optiuneDupaValoare, optiuniPiata, piataDupaGrup, valoareOptiune } from '../../lib/piete';
import { ChipMeniu } from '../ui/ChipMeniu';

interface Props {
  grup: string;
  cod: CodPiata;
  linie: number | null;
  onSchimba: (cod: CodPiata, linie: number | null) => void;
  /** Doar la piețele-interval: limita de sus („sub Y"); `linie` e limita de jos („peste X"). */
  linieSus?: number | null;
  onInterval?: (jos: number, sus: number) => void;
}

/**
 * Selecția din interiorul tipului de piață (direcție + linie), ca un chip cu meniu propriu, rotunjit
 * ca celelalte filtre (lista nativă a browserului nu se poate stiliza).
 * La piețele-interval chip-ul alege repriza, iar alături apar „Peste X" și „Sub Y".
 */
export function SelectorPiata({ grup, cod, linie, onSchimba, linieSus = null, onInterval }: Props) {
  const optiuni = optiuniPiata(grup);
  const { linii, interval } = piataDupaGrup(grup);
  const cuInterval = interval === true && onInterval != null && linie != null && linieSus != null;
  return (
    <>
      <ChipSelect
        eticheta="Piața"
        valoare={valoareOptiune(cod, cuInterval ? null : linie)}
        onSchimba={(v) => {
          const optiune = optiuneDupaValoare(grup, v);
          onSchimba(optiune.cod, cuInterval ? linie : optiune.linie);
        }}
        optiuni={optiuni.map((o) => ({ valoare: o.valoare, eticheta: o.eticheta }))}
      />
      {cuInterval && (
        <>
          <ChipSelect
            eticheta="Peste"
            prefix="Peste"
            latime="w-36"
            valoare={String(linie)}
            onSchimba={(v) => onInterval(Number(v), linieSus)}
            optiuni={linii.filter((l) => l < linieSus).map((l) => ({ valoare: String(l), eticheta: String(l) }))}
          />
          <ChipSelect
            eticheta="Sub"
            prefix="Sub"
            latime="w-36"
            valoare={String(linieSus)}
            onSchimba={(v) => onInterval(linie, Number(v))}
            optiuni={linii.filter((l) => l > linie).map((l) => ({ valoare: String(l), eticheta: String(l) }))}
          />
        </>
      )}
    </>
  );
}

function ChipSelect({
  eticheta,
  prefix,
  valoare,
  onSchimba,
  optiuni,
  latime = 'w-56',
}: {
  eticheta: string;
  prefix?: string;
  valoare: string;
  onSchimba: (valoare: string) => void;
  optiuni: { valoare: string; eticheta: string }[];
  latime?: string;
}) {
  const curenta = optiuni.find((o) => o.valoare === valoare)?.eticheta ?? '';
  return (
    <ChipMeniu
      titlu={eticheta}
      eticheta={
        <>
          {prefix && <span className="text-ink2">{prefix} </span>}
          {curenta}
        </>
      }
      latime={latime}
    >
      {(inchide) => (
        <div role="listbox" aria-label={eticheta} className="max-h-72 space-y-0.5 overflow-y-auto">
          {optiuni.map((o) => {
            const ales = o.valoare === valoare;
            return (
              <button
                key={o.valoare}
                type="button"
                role="option"
                aria-selected={ales}
                data-valoare={o.valoare}
                onClick={() => {
                  onSchimba(o.valoare);
                  inchide();
                }}
                className={`flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
                  ales ? 'bg-piata/15 text-ink' : 'text-ink2 hover:bg-ink2/10 hover:text-ink'
                }`}
              >
                <span className="truncate">{o.eticheta}</span>
                {ales && <span aria-hidden="true" className="text-piata">✓</span>}
              </button>
            );
          })}
        </div>
      )}
    </ChipMeniu>
  );
}
