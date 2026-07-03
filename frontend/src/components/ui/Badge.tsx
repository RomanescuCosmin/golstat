import type { ReactNode } from 'react';

type BadgeVariant = 'live' | 'primary' | 'neutral' | 'win' | 'draw' | 'loss';

const stiluri: Record<BadgeVariant, string> = {
  live: 'bg-accent/10 text-accent dark:bg-accent/15',
  primary: 'bg-primary/10 text-primary dark:bg-primary/15',
  neutral: 'bg-ink2/10 text-ink2 dark:bg-ink2/15',
  win: 'bg-win text-white',
  draw: 'bg-draw text-white',
  loss: 'bg-accent text-white',
};

interface BadgeProps {
  variant?: BadgeVariant;
  className?: string;
  children: ReactNode;
}

export function Badge({ variant = 'neutral', className = '', children }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${stiluri[variant]} ${className}`}
    >
      {variant === 'live' && <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />}
      {children}
    </span>
  );
}
