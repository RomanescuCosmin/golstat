import { useEffect, useState } from 'react';

export function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Numara de la 0 la `target` cu requestAnimationFrame (ease-out), la montare si la fiecare
 * schimbare de target. Cu prefers-reduced-motion intoarce direct tinta, fara animatie.
 */
export function useCountUp(target: number, durationMs = 600): number {
  const tinta = Number.isFinite(target) ? Math.round(target) : 0;
  const [valoare, setValoare] = useState(() => (prefersReducedMotion() ? tinta : 0));

  useEffect(() => {
    if (prefersReducedMotion() || tinta === 0) {
      setValoare(tinta);
      return;
    }
    let raf = 0;
    const start = performance.now();
    const pas = (acum: number) => {
      const t = Math.min((acum - start) / durationMs, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      setValoare(Math.round(eased * tinta));
      if (t < 1) raf = requestAnimationFrame(pas);
    };
    raf = requestAnimationFrame(pas);
    return () => cancelAnimationFrame(raf);
  }, [tinta, durationMs]);

  return valoare;
}
