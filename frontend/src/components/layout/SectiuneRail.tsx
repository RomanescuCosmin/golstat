import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../ui/Card';

interface SectiuneRailProps {
  titlu: string;
  linkText?: string;
  linkTo?: string;
  children: ReactNode;
}

/** O sectiune reutilizabila din rail-ul drept: antet (titlu majuscul + link optional) + continut. */
export function SectiuneRail({ titlu, linkText, linkTo, children }: SectiuneRailProps) {
  return (
    <Card>
      <div className="flex items-center justify-between border-b border-line px-4 py-3">
        <h2 className="text-sm font-extrabold uppercase tracking-wide text-ink">{titlu}</h2>
        {linkText && linkTo && (
          <Link to={linkTo} className="text-xs font-semibold text-accent hover:underline">
            {linkText}
          </Link>
        )}
      </div>
      {children}
    </Card>
  );
}
