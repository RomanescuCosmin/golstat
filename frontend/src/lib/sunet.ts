// Sunet de gol sintetizat prin Web Audio — fara fisier, merge offline, fara probleme de CSP.

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

/** Un mic arpegiu ascendent (Do–Mi–Sol) = „gol". Ignora orice eroare (audio blocat/indisponibil). */
export function sunetGol(): void {
  const c = context();
  if (!c) return;
  if (c.state === 'suspended') {
    void c.resume();
  }
  const acum = c.currentTime;
  const note = [523.25, 659.25, 783.99];
  for (let i = 0; i < note.length; i++) {
    const osc = c.createOscillator();
    const gain = c.createGain();
    osc.type = 'triangle';
    osc.frequency.value = note[i]!;
    const t = acum + i * 0.12;
    gain.gain.setValueAtTime(0.0001, t);
    gain.gain.exponentialRampToValueAtTime(0.25, t + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.26);
    osc.connect(gain).connect(c.destination);
    osc.start(t);
    osc.stop(t + 0.3);
  }
}
