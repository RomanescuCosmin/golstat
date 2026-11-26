import type { ReactNode } from 'react';

interface PageLayoutProps {
  children: ReactNode;
  /** Coloana contextuala: in dreapta pe xl+, sub continut (pe doua coloane la tableta) mai jos de xl. */
  rightRail?: ReactNode;
}

/** Containerul de continut al paginilor: coloana principala + rail optional, centrate pe latime. */
export function PageLayout({ children, rightRail }: PageLayoutProps) {
  return (
    <div className="mx-auto flex max-w-[1640px] flex-col items-stretch gap-5 xl:flex-row xl:items-start xl:gap-6">
      <div className="min-w-0 flex-1">{children}</div>
      {rightRail && (
        <aside className="grid min-w-0 gap-5 md:grid-cols-2 xl:w-80 xl:shrink-0 xl:grid-cols-1">{rightRail}</aside>
      )}
    </div>
  );
}
