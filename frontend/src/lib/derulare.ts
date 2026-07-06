import { prefersReducedMotion } from '../hooks/useCountUp';

const animari = new WeakMap<HTMLElement, number>();

/**
 * Deruleaza lin un container orizontal spre `scrollLeft + delta`, cu pornire si oprire line
 * (acelasi feel cu caruselul live). O animatie noua o anuleaza pe cea in curs.
 */
export function deruleazaLin(el: HTMLElement, delta: number, durataMs = 450): void {
  const de = el.scrollLeft;
  const tinta = Math.max(0, Math.min(de + delta, el.scrollWidth - el.clientWidth));

  if (prefersReducedMotion()) {
    el.scrollLeft = tinta;
    return;
  }

  const vechi = animari.get(el);
  if (vechi != null) cancelAnimationFrame(vechi);

  const t0 = performance.now();
  const pas = (t: number) => {
    const p = Math.min(1, (t - t0) / durataMs);
    const e = p < 0.5 ? 2 * p * p : 1 - (-2 * p + 2) ** 2 / 2;
    el.scrollLeft = de + (tinta - de) * e;
    if (p < 1) {
      animari.set(el, requestAnimationFrame(pas));
    } else {
      animari.delete(el);
    }
  };
  animari.set(el, requestAnimationFrame(pas));
}
