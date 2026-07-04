import type { SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement>;

function base(props: IconProps): IconProps {
  return {
    width: 20,
    height: 20,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
    ...props,
  };
}

export function IconCalendar(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18M8 3v4M16 3v4" />
    </svg>
  );
}

export function IconLive(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="2" fill="currentColor" stroke="none" />
      <path d="M8.5 8.5a5 5 0 0 0 0 7M15.5 8.5a5 5 0 0 1 0 7" />
      <path d="M5.6 5.6a9 9 0 0 0 0 12.8M18.4 5.6a9 9 0 0 1 0 12.8" />
    </svg>
  );
}

export function IconTrophy(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M8 4h8v5a4 4 0 0 1-8 0V4Z" />
      <path d="M8 5H5a3 3 0 0 0 3 5M16 5h3a3 3 0 0 1-3 5" />
      <path d="M12 13v4M9 20h6M10 17h4" />
    </svg>
  );
}

export function IconShield(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3Z" />
    </svg>
  );
}

export function IconUser(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20a7 7 0 0 1 14 0" />
    </svg>
  );
}

export function IconChart(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M5 20V12M10 20V6M15 20v-4M20 20V9" />
    </svg>
  );
}

export function IconStar(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9L12 3.5Z" />
    </svg>
  );
}

export function IconGear(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="3" />
      <path d="M12 2.8l1 2.4 2.6-.5 1.7 2-1.4 2.2 1.4 2.2-1.7 2 1.4 2.2-1.7 2-2.6-.5-1 2.4h-1.4l-1-2.4-2.6.5-1.7-2 1.4-2.2-1.4-2.2 1.7-2-1.4-2.2 1.7-2 2.6.5 1-2.4h1.4Z" />
    </svg>
  );
}

export function IconSearch(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="11" cy="11" r="6.5" />
      <path d="M20 20l-4-4" />
    </svg>
  );
}

export function IconSun(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5 5l1.8 1.8M17.2 17.2L19 19M19 5l-1.8 1.8M6.8 17.2L5 19" />
    </svg>
  );
}

export function IconMoon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M20 13.5A8 8 0 0 1 10.5 4 8 8 0 1 0 20 13.5Z" />
    </svg>
  );
}

export function IconCheck(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4.5 12.5l5 5 10-11" />
    </svg>
  );
}

export function IconDiamond(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M6 4h12l4 5-10 12L2 9l4-5Z" />
      <path d="M2 9h20M9 4l3 5 3-5M12 9l0 12" strokeWidth={1.2} />
    </svg>
  );
}

export function IconChevronRight(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M9 5l7 7-7 7" />
    </svg>
  );
}

export function IconChevronLeft(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M15 5l-7 7 7 7" />
    </svg>
  );
}

export function IconChevronDown(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M5 9l7 7 7-7" />
    </svg>
  );
}

export function IconGlobe(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="9" />
      <path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18" />
    </svg>
  );
}

/** Minge de fotbal — folosita pentru evenimentul "gol" din cronologie. */
export function IconBall(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5l3.2 2.3-1.2 3.7h-4l-1.2-3.7L12 7.5Z" strokeWidth={1.4} />
      <path d="M12 3v2m6.4 2.8-1.7 1.3m2.3 5.6-2-.7M6.9 16l-2 .7M5.3 7.8 7 9.1" strokeWidth={1.2} />
    </svg>
  );
}

/** Dreptunghi vertical (cartonas) — evenimentul "cartonas" din cronologie; culoarea vine din className. */
export function IconCartonas(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="7" y="3.5" width="10" height="14" rx="1.5" fill="currentColor" stroke="none" transform="rotate(8 12 10.5)" />
    </svg>
  );
}

/** Sageti opuse (schimbare de jucator) — evenimentul "subst" din cronologie. */
export function IconSchimbare(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4 8h11l-3-3M20 16H9l3 3" />
    </svg>
  );
}

/** Ecran (VAR) — evenimentul "Var" din cronologie. */
export function IconVar(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="3" y="5" width="18" height="12" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </svg>
  );
}

/** Fluier — folosit pentru arbitru / informatii oficiale. */
export function IconFluier(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3 10h9l6-2v6a5 5 0 0 1-10 0v-1H3v-3Z" />
      <circle cx="8" cy="13" r="1.4" fill="currentColor" stroke="none" />
    </svg>
  );
}
