import type { ButtonHTMLAttributes } from 'react';

interface ButonIconProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  eticheta: string;
}

/** Buton rotund doar cu iconita, cu tinta de atingere de 40px (header, actiuni din carduri). */
export function ButonIcon({ eticheta, className = '', children, ...rest }: ButonIconProps) {
  return (
    <button
      type="button"
      title={eticheta}
      aria-label={eticheta}
      className={`relative flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-ink2 transition hover:bg-ink2/10 hover:text-ink active:scale-95 ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}
