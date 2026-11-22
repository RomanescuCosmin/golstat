const PRAGURI_RAPIDE = [30, 50, 60, 70, 80];

/**
 * Pragul minim de probabilitate (0..100), în meniul chip-ului „Șansă". Sub el, meciurile nu mai
 * apar în listă. Partea parcursă se colorează printr-un gradient calculat din valoare —
 * `accent-color` colorează doar butonul, nu și traseul din stânga lui.
 */
export function PragSlider({ prag, onSchimba }: { prag: number; onSchimba: (v: number) => void }) {
  return (
    <div className="flex flex-col gap-3">
      <label
        htmlFor="prag-piete"
        className="flex items-baseline justify-between gap-3 text-[11px] font-bold uppercase tracking-[0.07em] text-ink2"
      >
        Șansă minimă
        <span className="text-base font-extrabold normal-case tracking-normal tabular-nums text-ink">
          {prag}%
        </span>
      </label>
      <input
        id="prag-piete"
        type="range"
        min={0}
        max={100}
        step={5}
        value={prag}
        aria-label="Șansă minimă"
        aria-valuenow={prag}
        aria-valuetext={`${prag}%`}
        onChange={(e) => onSchimba(Number(e.target.value))}
        style={{
          background: `linear-gradient(to right, rgb(var(--gs-piata)) ${prag}%, rgb(var(--gs-ink2) / 0.2) ${prag}%)`,
        }}
        className="h-1.5 w-full cursor-pointer appearance-none rounded-full accent-piata"
      />
      <div className="flex gap-1.5" role="group" aria-label="Praguri rapide">
        {PRAGURI_RAPIDE.map((p) => (
          <button
            key={p}
            type="button"
            aria-pressed={p === prag}
            onClick={() => onSchimba(p)}
            className={`flex-1 rounded-lg py-1.5 text-xs font-bold tabular-nums transition ${
              p === prag ? 'bg-piata text-white' : 'bg-bg text-ink2 hover:text-ink'
            }`}
          >
            {p}%
          </button>
        ))}
      </div>
    </div>
  );
}
