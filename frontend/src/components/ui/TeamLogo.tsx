import { useState } from 'react';

interface TeamLogoProps {
  nume?: string | null;
  logo?: string | null;
  size?: number;
  className?: string;
}

function initiale(nume: string | null | undefined): string {
  if (!nume) {
    return '?';
  }
  const cuvinte = nume.split(/\s+/).filter(Boolean);
  const rezultat = cuvinte
    .slice(0, 2)
    .map((c) => c[0]!.toUpperCase())
    .join('');
  return rezultat || '?';
}

/** Logo de echipa cu fallback: daca imaginea (URL extern api-sports) pica, afisam initialele. */
export function TeamLogo({ nume, logo, size = 32, className = '' }: TeamLogoProps) {
  const [failed, setFailed] = useState(false);

  if (!logo || failed) {
    return (
      <span
        style={{ width: size, height: size, fontSize: Math.max(10, size * 0.34) }}
        className={`inline-flex shrink-0 items-center justify-center rounded-full bg-primary/10 font-bold text-primary dark:bg-primary/20 ${className}`}
        title={nume ?? undefined}
      >
        {initiale(nume)}
      </span>
    );
  }

  return (
    <img
      src={logo}
      alt={nume ?? 'Echipă'}
      width={size}
      height={size}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`shrink-0 object-contain ${className}`}
    />
  );
}
