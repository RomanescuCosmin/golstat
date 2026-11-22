// Sunetul de gol: „puls dublu” sintetizat cu Web Audio.

let ctx: AudioContext | null = null;

function context(): AudioContext | null {
  try {
    if (!ctx) {
      const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctor) return null;
      ctx = new Ctor();
    }
    return ctx;
  } catch {
    return null;
  }
}

/**
 * Deblocheaza contextul audio la primul gest al utilizatorului. Browserele blocheaza sunetul pana
 * la o interactiune; apeleaza asta pe un click/tasta ca sunetul de gol sa poata porni ulterior.
 */
export function pregatesteSunet(): void {
  const c = context();
  if (c && c.state === 'suspended') {
    void c.resume();
  }
}

/** Doua impulsuri moi, joase-inalte, cu o mica coada. Ignora orice eroare. */
export function sunetGol(): void {
  const c = context();
  if (!c) return;
  if (c.state === 'suspended') {
    void c.resume();
  }
  const acum = c.currentTime;
  for (const [start, frecventa] of [[0, 659.25], [0.16, 987.77]] as const) {
    const t = acum + start;
    const osc = c.createOscillator();
    osc.type = 'triangle';
    osc.frequency.value = frecventa;
    const gain = c.createGain();
    gain.gain.setValueAtTime(0.0001, t);
    gain.gain.exponentialRampToValueAtTime(0.25, t + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.3);
    osc.connect(gain).connect(c.destination);
    osc.start(t);
    osc.stop(t + 0.32);
  }
}
