import type { CodPiata } from '../../api/types';
import { piataDupaGrup, type OptiuneLiga } from '../../lib/piete';
import { ChipMeniu } from '../ui/ChipMeniu';
import { PragSlider } from './PragSlider';
import { SegmenteTipPiata } from './SegmenteTipPiata';
import { SelectorLigi } from './SelectorLigi';
import { SelectorPiata } from './SelectorPiata';

interface Props {
  grup: string;
  cod: CodPiata;
  linie: number | null;
  linieSus?: number | null;
  prag: number;
  ligi: OptiuneLiga[];
  ligiSelectate: number[];
  onGrup: (grup: string) => void;
  onPiata: (cod: CodPiata, linie: number | null) => void;
  onPrag: (prag: number) => void;
  onLigi: (selectate: number[]) => void;
  onInterval?: (jos: number, sus: number) => void;
}

const PRAG_IMPLICIT = 30;

function etichetaLigi(ligi: OptiuneLiga[], selectate: number[]): string {
  if (selectate.length === 0) return 'Toate campionatele';
  if (selectate.length === 1) {
    const liga = ligi.find((l) => l.id === selectate[0]);
    return liga?.nume ?? '1 campionat';
  }
  return `${selectate.length} campionate`;
}

/**
 * Bara de filtre: un singur rând de chip-uri, în ordinea în care se ia decizia — tipul pieței,
 * piața exactă, cât de sigură, unde. Fiecare chip arată alegerea curentă; „Tip", „Șansă" și
 * „Campionate" deschid un meniu mic, piața e un dropdown nativ. Rândul se împachetează pe ecrane
 * înguste, deci nu mai există scroll orizontal.
 */
export function FiltrePiete({
  grup,
  cod,
  linie,
  linieSus,
  prag,
  ligi,
  ligiSelectate,
  onGrup,
  onPiata,
  onPrag,
  onLigi,
  onInterval,
}: Props) {
  const tip = piataDupaGrup(grup);
  return (
    <div className="flex flex-wrap items-center gap-2">
      <ChipMeniu titlu="Tip piață" eticheta={tip.eticheta} icona={tip.icona} activ latime="w-[22rem]">
        {(inchide) => (
          <SegmenteTipPiata
            grup={grup}
            onSchimba={(g) => {
              onGrup(g);
              inchide();
            }}
          />
        )}
      </ChipMeniu>

      <SelectorPiata grup={grup} cod={cod} linie={linie} onSchimba={onPiata} linieSus={linieSus}
        onInterval={onInterval} />

      <ChipMeniu titlu="Șansă minimă" eticheta={`Șansă ≥ ${prag}%`} activ={prag !== PRAG_IMPLICIT}>
        {() => <PragSlider prag={prag} onSchimba={onPrag} />}
      </ChipMeniu>

      <ChipMeniu titlu="Campionate" eticheta={etichetaLigi(ligi, ligiSelectate)} activ={ligiSelectate.length > 0}>
        {() => <SelectorLigi ligi={ligi} selectate={ligiSelectate} onSchimba={onLigi} />}
      </ChipMeniu>
    </div>
  );
}
