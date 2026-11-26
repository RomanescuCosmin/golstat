import { ChipMeniu } from './ChipMeniu';

/** Un chip cu listă de opțiuni rotunjită (în locul select-ului nativ, care nu se poate stiliza). */
export function ChipSelect({
  eticheta,
  prefix,
  valoare,
  onSchimba,
  optiuni,
  latime = 'w-56',
}: {
  eticheta: string;
  prefix?: string;
  valoare: string;
  onSchimba: (valoare: string) => void;
  optiuni: { valoare: string; eticheta: string }[];
  latime?: string;
}) {
  const curenta = optiuni.find((o) => o.valoare === valoare)?.eticheta ?? '';
  return (
    <ChipMeniu
      titlu={eticheta}
      eticheta={
        <>
          {prefix && <span className="text-ink2">{prefix} </span>}
          {curenta}
        </>
      }
      latime={latime}
    >
      {(inchide) => (
        <div role="listbox" aria-label={eticheta} className="max-h-72 space-y-0.5 overflow-y-auto">
          {optiuni.map((o) => {
            const ales = o.valoare === valoare;
            return (
              <button
                key={o.valoare}
                type="button"
                role="option"
                aria-selected={ales}
                data-valoare={o.valoare}
                onClick={() => {
                  onSchimba(o.valoare);
                  inchide();
                }}
                className={`flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
                  ales ? 'bg-piata/15 text-ink' : 'text-ink2 hover:bg-ink2/10 hover:text-ink'
                }`}
              >
                <span className="truncate">{o.eticheta}</span>
                {ales && <span aria-hidden="true" className="text-piata">✓</span>}
              </button>
            );
          })}
        </div>
      )}
    </ChipMeniu>
  );
}
