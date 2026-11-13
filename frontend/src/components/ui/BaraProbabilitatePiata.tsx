import { useGrowOnMount } from '../../hooks/useAnimatii';

/**
 * Culoarea unei probabilitati de piata pe pagina de meci: >= 70% verde aprins, 50–69% galben,
 * sub 50% rosu. Aceleasi praguri pentru bara si pentru cifra de langa ea.
 */
export function culoareProbabilitate(rata: number): { text: string; bara: string } {
  const procent = Number.isFinite(rata) ? rata * 100 : 0;
  if (procent >= 70) {
    return { text: 'text-green-500', bara: 'bg-green-500' };
  }
  if (procent >= 50) {
    return { text: 'text-yellow-600 dark:text-yellow-500', bara: 'bg-yellow-500' };
  }
  return { text: 'text-red-500', bara: 'bg-red-500' };
}

/**
 * Bară de probabilitate a unei piețe: umplere colorată după prag + marker la 50%.
 *
 * Fără istoric (`areIstoric={false}`) rămâne DOAR track gol — un progress bar plin fără date în
 * spate ar minți. Valorile ne-finite se tratează la fel ca 0.
 *
 * Nu se confundă cu `components/meciuri/BaraProbabilitate` (bara segmentată 1X2).
 */
export function BaraProbabilitatePiata({
  rata,
  areIstoric = true,
  varianta = 'model',
  className = '',
}: {
  rata: number;
  areIstoric?: boolean;
  /**
   * `model` = verde/galben/roșu după prag + marker la 50%, pe pagina de meci.
   * `sansa` = verde, în lista de piețe, unde bara e singurul indicator vizual al rândului.
   */
  varianta?: 'model' | 'sansa';
  className?: string;
}) {
  const montat = useGrowOnMount();
  const procent = Number.isFinite(rata) ? Math.max(0, Math.min(100, rata * 100)) : 0;
  const umplere = varianta === 'sansa' ? 'bg-win' : culoareProbabilitate(rata).bara;
  return (
    <div
      className={`relative h-3 flex-1 overflow-hidden rounded-full bg-ink2/10 dark:bg-ink2/15 ${className}`}
    >
      {areIstoric && (
        <div
          data-testid="umplere-bara"
          className={`h-full rounded-full transition-[width] duration-700 ease-out motion-reduce:transition-none ${umplere}`}
          style={{ width: montat ? `${procent}%` : '0%' }}
        />
      )}
      {varianta === 'model' && (
        <span className="absolute inset-y-0 left-1/2 w-px -translate-x-1/2 bg-ink2/30" aria-hidden />
      )}
    </div>
  );
}
