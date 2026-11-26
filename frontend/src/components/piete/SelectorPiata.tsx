import type { CodPiata } from '../../api/types';
import {
  delaDinLinie,
  optiuneDupaValoare,
  optiuniPiata,
  panaLaDinLinie,
  piataDupaGrup,
  valoareOptiune,
} from '../../lib/piete';
import { ChipSelect } from '../ui/ChipSelect';

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
  const { linii, interval, intervalIntreg } = piataDupaGrup(grup);
  const cuInterval = interval === true && onInterval != null && linie != null && linieSus != null;
  // la intervalele întregi („de la 1 până la 3”) limitele se afișează ca numere întregi
  const jos = intervalIntreg ? { titlu: 'De la', text: delaDinLinie } : { titlu: 'Peste', text: (l: number) => l };
  const sus = intervalIntreg ? { titlu: 'Până la', text: panaLaDinLinie } : { titlu: 'Sub', text: (l: number) => l };
  return (
    <>
      {!(intervalIntreg && optiuni.length === 1) && (
        <ChipSelect
          eticheta="Piața"
          valoare={valoareOptiune(cod, cuInterval ? null : linie)}
          onSchimba={(v) => {
            const optiune = optiuneDupaValoare(grup, v);
            onSchimba(optiune.cod, cuInterval ? linie : optiune.linie);
          }}
          optiuni={optiuni.map((o) => ({ valoare: o.valoare, eticheta: o.eticheta }))}
        />
      )}
      {cuInterval && (
        <>
          <ChipSelect
            eticheta={jos.titlu}
            prefix={jos.titlu}
            latime="w-36"
            valoare={String(linie)}
            onSchimba={(v) => onInterval(Number(v), linieSus)}
            optiuni={linii
              .filter((l) => l < linieSus)
              .map((l) => ({ valoare: String(l), eticheta: String(jos.text(l)) }))}
          />
          <ChipSelect
            eticheta={sus.titlu}
            prefix={sus.titlu}
            latime="w-36"
            valoare={String(linieSus)}
            onSchimba={(v) => onInterval(linie, Number(v))}
            optiuni={linii
              .filter((l) => l > linie)
              .map((l) => ({ valoare: String(l), eticheta: String(sus.text(l)) }))}
          />
        </>
      )}
    </>
  );
}

