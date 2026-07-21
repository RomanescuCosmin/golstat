import type { CodPiata } from '../../api/types';
import { optiuneDupaValoare, optiuniPiata, valoareOptiune } from '../../lib/piete';

interface Props {
  grup: string;
  cod: CodPiata;
  linie: number | null;
  onSchimba: (cod: CodPiata, linie: number | null) => void;
}

/**
 * Selecția din interiorul unui tip de piață (direcție + linie), ca un singur dropdown.
 *
 * Înainte erau două rânduri de pastile — direcția și linia — deci până la opt butoane pentru o
 * alegere care e, de fapt, una singură. Lista se schimbă odată cu tipul: la Cornere vezi 7.5–10.5,
 * la Cartonașe 3.5–5.5.
 */
export function SelectorPiata({ grup, cod, linie, onSchimba }: Props) {
  const optiuni = optiuniPiata(grup);
  return (
    <div className="flex min-w-0 flex-col gap-2 sm:w-64">
      <label
        htmlFor="selector-piata"
        className="text-[11px] font-bold uppercase tracking-[0.07em] text-ink2"
      >
        Piața
      </label>
      <div className="relative">
        <select
          id="selector-piata"
          value={valoareOptiune(cod, linie)}
          onChange={(e) => {
            const optiune = optiuneDupaValoare(grup, e.target.value);
            onSchimba(optiune.cod, optiune.linie);
          }}
          className="h-11 w-full cursor-pointer appearance-none rounded-input border border-line bg-card pl-3.5 pr-10 text-sm font-semibold text-ink transition hover:border-ink2/40 focus:border-piata focus:outline-none focus:ring-2 focus:ring-piata/30"
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
          className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-ink2"
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </div>
    </div>
  );
}
