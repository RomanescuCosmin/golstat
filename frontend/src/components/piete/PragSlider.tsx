/**
 * Pragul minim de probabilitate (0..100). Sub el, meciurile nu mai apar în listă.
 *
 * Partea parcursă se colorează cu albastrul de brand printr-un gradient calculat din valoare —
 * `accent-color` colorează doar butonul, nu și traseul din stânga lui.
 */
export function PragSlider({ prag, onSchimba }: { prag: number; onSchimba: (v: number) => void }) {
  return (
    <div className="flex min-w-0 flex-1 flex-col gap-2">
      <label
        htmlFor="prag-piete"
        className="flex items-baseline justify-between gap-3 text-[11px] font-bold uppercase tracking-[0.07em] text-ink2"
      >
        Șansă minimă
        <span className="text-[13px] font-extrabold normal-case tracking-normal tabular-nums text-ink">
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
      <div className="flex justify-between text-[11px] font-semibold tabular-nums text-ink2">
        <span>0%</span>
        <span>100%</span>
      </div>
    </div>
  );
}
