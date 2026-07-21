import type { CodPiata } from '../../api/types';
import type { OptiuneLiga } from '../../lib/piete';
import { PragSlider } from './PragSlider';
import { SegmenteTipPiata } from './SegmenteTipPiata';
import { SelectorLigi } from './SelectorLigi';
import { SelectorPiata } from './SelectorPiata';

interface Props {
  grup: string;
  cod: CodPiata;
  linie: number | null;
  prag: number;
  ligi: OptiuneLiga[];
  ligiSelectate: number[];
  onGrup: (grup: string) => void;
  onPiata: (cod: CodPiata, linie: number | null) => void;
  onPrag: (prag: number) => void;
  onLigi: (selectate: number[]) => void;
}

/**
 * Zona de filtre: tipul pieței (segmente), selecția din interiorul ei (dropdown), pragul minim
 * și campionatele. Trei rânduri, în ordinea în care se ia decizia — ce piață, cât de sigură,
 * unde. Pe ecrane mici, segmentele și campionatele derulează orizontal în loc să se împacheteze.
 */
export function FiltrePiete({
  grup,
  cod,
  linie,
  prag,
  ligi,
  ligiSelectate,
  onGrup,
  onPiata,
  onPrag,
  onLigi,
}: Props) {
  return (
    <div className="flex flex-col gap-4">
      <div className="-mx-1 overflow-x-auto px-1 pb-1">
        <SegmenteTipPiata grup={grup} onSchimba={onGrup} />
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:gap-8">
        <SelectorPiata grup={grup} cod={cod} linie={linie} onSchimba={onPiata} />
        <PragSlider prag={prag} onSchimba={onPrag} />
      </div>

      <div className="border-t border-line pt-4">
        <SelectorLigi ligi={ligi} selectate={ligiSelectate} onSchimba={onLigi} />
      </div>
    </div>
  );
}
