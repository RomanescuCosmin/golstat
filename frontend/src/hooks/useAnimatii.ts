import { useEffect, useRef, useState } from 'react';
import { prefersReducedMotion } from './useCountUp';

/**
 * `false` la primul render, `true` dupa primul frame — barele pornesc de la 0 si cresc
 * spre latimea tinta prin `transition-[width]`. Cu prefers-reduced-motion e `true` direct.
 */
export function useGrowOnMount(): boolean {
  const [montat, setMontat] = useState(() => prefersReducedMotion());

  useEffect(() => {
    if (montat) return;
    const raf = requestAnimationFrame(() => setMontat(true));
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return montat;
}

/** `true` pentru scurt timp cand `scor` se schimba fata de valoarea anterioara (nu la montare). */
export function useScoreFlash(scor: string, durationMs = 900): boolean {
  const anterior = useRef<string | null>(null);
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    if (anterior.current !== null && anterior.current !== scor && !prefersReducedMotion()) {
      setFlash(true);
      const t = setTimeout(() => setFlash(false), durationMs);
      anterior.current = scor;
      return () => clearTimeout(t);
    }
    anterior.current = scor;
  }, [scor, durationMs]);

  return flash;
}
