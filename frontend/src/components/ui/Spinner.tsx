import { MingeGolStat } from './MingeGolStat';

interface SpinnerProps {
  size?: number;
  className?: string;
}

/** Loader-ul aplicatiei = marca GolStat care se roteste (element de brand recurent). */
export function Spinner({ size = 28, className = '' }: SpinnerProps) {
  return (
    <span role="status" aria-label="Se încarcă" className={`inline-flex ${className}`}>
      <MingeGolStat size={size} spinning />
    </span>
  );
}
