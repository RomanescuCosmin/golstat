import type { ReactNode } from 'react';

interface RandSetareProps {
  icon: ReactNode;
  titlu: string;
  descriere?: ReactNode;
  children?: ReactNode;
}

/** Rand de lista in stil aplicatie mobila: iconita, titlu + descriere, control in dreapta. */
export function RandSetare({ icon, titlu, descriere, children }: RandSetareProps) {
  return (
    <div className="flex min-h-[60px] items-center gap-3 px-4 py-3">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-primary">{icon}</span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-ink">{titlu}</p>
        {descriere && <p className="text-xs leading-snug text-ink2">{descriere}</p>}
      </div>
      {children && <div className="shrink-0">{children}</div>}
    </div>
  );
}
