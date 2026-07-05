import type { Predictie1X2 } from '../../api/types';
import { useGrowOnMount } from '../../hooks/useAnimatii';

/**
 * Bara segmentata 1X2 (albastru = victorie gazde, gri = egal, rosu = victorie oaspeti) cu procentele
 * dedesubt. Un singur element vizual, conform design system-ului: airy, doar accente albastru/rosu.
 */
export function BaraProbabilitate({ predictie }: { predictie: Predictie1X2 }) {
  const g = Math.round(predictie.gazde.procent);
  const e = Math.round(predictie.egal.procent);
  const o = Math.round(predictie.oaspeti.procent);
  const montat = useGrowOnMount();
  const seg = 'transition-[width] duration-500 ease-out motion-reduce:transition-none';

  return (
    <div>
      <div className="flex h-2 overflow-hidden rounded-full bg-line">
        <div className={`bg-primary ${seg}`} style={{ width: montat ? `${g}%` : '0%' }} />
        <div className={`bg-draw ${seg}`} style={{ width: montat ? `${e}%` : '0%' }} />
        <div className={`bg-accent ${seg}`} style={{ width: montat ? `${o}%` : '0%' }} />
      </div>
      <div className="mt-1.5 flex justify-between text-[11px] font-semibold tabular-nums">
        <span className="text-primary">{g}%</span>
        <span className="text-ink2">{e}%</span>
        <span className="text-accent">{o}%</span>
      </div>
    </div>
  );
}
