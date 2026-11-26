interface ComutatorProps {
  activ: boolean;
  onSchimba: (activ: boolean) => void;
  eticheta: string;
}

/** Comutator on/off (role="switch"), dimensionat pentru atingere. */
export function Comutator({ activ, onSchimba, eticheta }: ComutatorProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={activ}
      aria-label={eticheta}
      onClick={() => onSchimba(!activ)}
      className={`relative inline-flex h-7 w-12 shrink-0 items-center rounded-full transition ${
        activ ? 'bg-primary' : 'bg-ink2/30'
      }`}
    >
      <span
        className={`inline-block h-5 w-5 rounded-full bg-white shadow transition-transform ${
          activ ? 'translate-x-6' : 'translate-x-1'
        }`}
      />
    </button>
  );
}
