import { useGrowOnMount } from '../../hooks/useAnimatii';

/**
 * Bară de probabilitate a unei piețe: umplere gradient + marker la 50%.
 *
 * Fără istoric (`areIstoric={false}`) rămâne DOAR track gol — un progress bar plin fără date în
 * spate ar minți. Valorile ne-finite se tratează la fel ca 0.
 *
 * Nu se confundă cu `components/meciuri/BaraProbabilitate` (bara segmentată 1X2).
 */
export function BaraProbabilitatePiata({
  rata,
  areIstoric = true,
}: {
  rata: number;
  areIstoric?: boolean;
}) {
  const montat = useGrowOnMount();
  const procent = Number.isFinite(rata) ? Math.max(0, Math.min(100, rata * 100)) : 0;
  const puternica = procent >= 50;
  return (
    <div className="relative h-3 flex-1 overflow-hidden rounded-full bg-ink2/10 dark:bg-ink2/15">
      {areIstoric && (
        <div
          data-testid="umplere-bara"
          className={`h-full rounded-full bg-gradient-to-r transition-[width] duration-700 ease-out motion-reduce:transition-none ${
            puternica ? 'from-primary/60 to-primary' : 'from-draw/60 to-draw'
          }`}
          style={{ width: montat ? `${procent}%` : '0%' }}
        />
      )}
      <span className="absolute inset-y-0 left-1/2 w-px -translate-x-1/2 bg-ink2/30" aria-hidden />
    </div>
  );
}
