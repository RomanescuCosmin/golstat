interface MingeGolStatProps {
  size?: number;
  className?: string;
  /** Cand e true, mingea se roteste continuu (spinner de incarcare — element de brand recurent). */
  spinning?: boolean;
}

/**
 * Marca GolStat: mingea de fotbal (albastru/roșu, contur navy) din `public/logo-minge.png`.
 * Centralizata ca sa apara identic peste tot (navbar, sidebar, favicon, spinner, splash).
 * `spinning` o roteste (loader); animatiile de hover se aplica de la parinte prin `className`.
 */
export function MingeGolStat({ size = 34, className = '', spinning = false }: MingeGolStatProps) {
  return (
    <img
      src="/logo-minge.png"
      width={size}
      height={size}
      alt=""
      aria-hidden
      draggable={false}
      className={`${spinning ? 'animate-spin' : ''} select-none ${className}`}
      style={{ width: size, height: size }}
    />
  );
}
