/** Pragul minim de probabilitate (0..100). Sub el, meciurile nu mai apar în listă. */
export function PragSlider({ prag, onSchimba }: { prag: number; onSchimba: (v: number) => void }) {
  return (
    <div className="flex min-w-[12rem] flex-1 items-center gap-3">
      <label htmlFor="prag-piete" className="shrink-0 text-xs font-semibold text-ink2">
        Șansă minimă
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
        className="h-1.5 flex-1 cursor-pointer appearance-none rounded-full bg-ink2/20 accent-primary"
      />
      <span className="w-10 shrink-0 text-right text-sm font-extrabold tabular-nums text-primary">
        {prag}%
      </span>
    </div>
  );
}
