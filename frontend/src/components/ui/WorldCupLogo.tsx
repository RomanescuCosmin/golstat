interface WorldCupLogoProps {
  size?: number;
  className?: string;
}

/**
 * Logo pentru Campionatul Mondial: un trofeu auriu clasic (cupa cu manere + baza), desenat de noi —
 * API-Football serveste doar un placeholder gri pentru liga 1. Culori aurii solide + un highlight de
 * luciu; fara gradient cu id, ca sa nu apara coliziuni cand se randeaza de mai multe ori pe pagina.
 */
export function WorldCupLogo({ size = 22, className = '' }: WorldCupLogoProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      className={`shrink-0 ${className}`}
      role="img"
      aria-label="Campionatul Mondial"
    >
      {/* manerele cupei */}
      <path d="M6.6 4.6 C3.9 4.6, 3.9 9.2, 7.4 9.4" fill="none" stroke="#C1861F" strokeWidth="1.3" strokeLinecap="round" />
      <path d="M17.4 4.6 C20.1 4.6, 20.1 9.2, 16.6 9.4" fill="none" stroke="#C1861F" strokeWidth="1.3" strokeLinecap="round" />
      {/* bolul cupei */}
      <path d="M6.4 3.4 H17.6 V6.1 C17.6 10.4, 15.1 13, 12 13 C8.9 13, 6.4 10.4, 6.4 6.1 Z" fill="#E3B23C" />
      {/* luciu */}
      <path d="M9 4.5 C8.5 7.4, 9.1 10, 10.4 11.7" fill="none" stroke="#F5D884" strokeWidth="1.1" strokeLinecap="round" opacity="0.85" />
      {/* stem */}
      <rect x="11.15" y="12.9" width="1.7" height="2.7" fill="#C1861F" />
      {/* baza (doua trepte) */}
      <rect x="9" y="15.4" width="6" height="1.5" rx="0.5" fill="#E3B23C" />
      <rect x="7.5" y="17" width="9" height="2.5" rx="0.9" fill="#C1861F" />
    </svg>
  );
}
