import type { HTMLAttributes } from 'react';

type CardProps = HTMLAttributes<HTMLDivElement>;

/** Cardul alb de baza al aplicatiei (fundal card, bordura fina, colturi rotunjite). */
export function Card({ className = '', children, ...rest }: CardProps) {
  return (
    <div
      className={`rounded-card border border-line bg-card shadow-card dark:shadow-none ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}
