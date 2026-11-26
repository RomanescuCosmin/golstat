import { Link } from 'react-router-dom';
import { MingeGolStat } from '../ui/MingeGolStat';

/** Marca GolStat (mingea + wordmark-ul gol/stat), aceeasi in toate header-ele. */
export function Logo({ marime = 34, faraText = false }: { marime?: number; faraText?: boolean }) {
  return (
    <Link to="/" className="group flex shrink-0 items-center gap-2" aria-label="golstat — acasă">
      <MingeGolStat
        size={marime}
        className="transition-transform duration-200 group-hover:rotate-[16deg] group-hover:scale-110 motion-reduce:transition-none"
      />
      {!faraText && (
        <span className="text-[22px] font-extrabold tracking-tight">
          <span className="text-primary">gol</span>
          <span className="text-accent">stat</span>
        </span>
      )}
    </Link>
  );
}
