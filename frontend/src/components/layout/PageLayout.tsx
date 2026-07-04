import type { ReactNode } from 'react';

interface PageLayoutProps {
  children: ReactNode;
  /** Continutul optional al coloanei din dreapta (rail contextual); ascuns sub `xl`. */
  rightRail?: ReactNode;
}

/**
 * Containerul de continut al paginilor interioare: coloana principala + un rail drept optional,
 * centrate cu gutter simetric (`mx-auto max-w-7xl`) intre sidebar-ul stang si marginea dreapta.
 * Doar TopNav-ul ramane pe toata latimea; restul paginilor stau in aceasta banda centrala.
 */
export function PageLayout({ children, rightRail }: PageLayoutProps) {
  return (
    <div className="mx-auto flex max-w-7xl items-start gap-5 lg:gap-6">
      <div className="min-w-0 flex-1">{children}</div>
      {rightRail && <aside className="hidden w-80 shrink-0 space-y-5 xl:block">{rightRail}</aside>}
    </div>
  );
}
