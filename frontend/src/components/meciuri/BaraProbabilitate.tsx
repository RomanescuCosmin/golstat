import type { Predictie1X2 } from '../../api/types';
import { useGrowOnMount } from '../../hooks/useAnimatii';

/**
 * Rotunjire cu "rest maxim": intregii insumeaza mereu 100, altfel rotunjirea independenta
 * poate da 99 (gol vizual in bara) sau 101 (bara depaseste).
 */
function procenteRotunjite(valori: number[]): number[] {
  const podele = valori.map(Math.floor);
  let rest = 100 - podele.reduce((a, b) => a + b, 0);
  const ordine = valori
    .map((v, i) => ({ frac: v - podele[i], i }))
    .sort((a, b) => b.frac - a.frac);
  for (const { i } of ordine) {
    if (rest <= 0) break;
    podele[i]++;
    rest--;
  }
  return podele;
}

/**
 * Bara segmentata 1X2 (albastru = victorie gazde, gri = egal, rosu = victorie oaspeti) cu procentele
 * dedesubt. Un singur element vizual, conform design system-ului: airy, doar accente albastru/rosu.
 */
export function BaraProbabilitate({ predictie }: { predictie: Predictie1X2 }) {
  const [g, e, o] = procenteRotunjite([
    predictie.gazde.procent,
    predictie.egal.procent,
    predictie.oaspeti.procent,
  ]);
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
