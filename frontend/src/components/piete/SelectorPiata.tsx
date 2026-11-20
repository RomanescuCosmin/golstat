import type { CodPiata } from '../../api/types';
import { optiuneDupaValoare, optiuniPiata, piataDupaGrup, valoareOptiune } from '../../lib/piete';
import { claseChip } from '../ui/ChipMeniu';

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
 * Selecția din interiorul tipului de piață (direcție + linie), ca un chip cu dropdown nativ — pe
 * telefon deschide selectorul sistemului. `color-scheme` face lista de opțiuni întunecată pe dark.
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
        id="selector-piata"
      />
      {cuInterval && (
        <>
          <ChipSelect
            eticheta="Peste"
            prefix="Peste"
            valoare={String(linie)}
            onSchimba={(v) => onInterval(Number(v), linieSus)}
            optiuni={linii.filter((l) => l < linieSus).map((l) => ({ valoare: String(l), eticheta: String(l) }))}
          />
          <ChipSelect
            eticheta="Sub"
            prefix="Sub"
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
  id,
}: {
  eticheta: string;
  prefix?: string;
  valoare: string;
  onSchimba: (valoare: string) => void;
  optiuni: { valoare: string; eticheta: string }[];
  id?: string;
}) {
  return (
    <label className={`${claseChip(false)} relative cursor-pointer pr-9 focus-within:ring-2 focus-within:ring-piata/40`}>
      {prefix && <span className="text-ink2">{prefix}</span>}
      <select
        id={id}
        aria-label={eticheta}
        value={valoare}
        onChange={(e) => onSchimba(e.target.value)}
        className="cursor-pointer appearance-none bg-transparent font-semibold text-ink [color-scheme:light] focus:outline-none dark:[color-scheme:dark]"
      >
        {optiuni.map((o) => (
          <option key={o.valoare} value={o.valoare}>
            {o.eticheta}
          </option>
        ))}
      </select>
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        width={14}
        height={14}
        fill="none"
        stroke="currentColor"
        strokeWidth={2.5}
        strokeLinecap="round"
        strokeLinejoin="round"
        className="pointer-events-none absolute right-3.5 text-ink2"
      >
        <path d="M6 9l6 6 6-6" />
      </svg>
    </label>
  );
}
