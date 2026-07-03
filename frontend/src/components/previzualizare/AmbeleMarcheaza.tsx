import type { ProcentCota } from '../../api/types';
import { Card } from '../ui/Card';

interface AmbeleMarcheazaProps {
  btts: ProcentCota;
}

/** "Ambele echipe marchează": Da (albastru) / Nu (rosu), sumand 100%. */
export function AmbeleMarcheaza({ btts }: AmbeleMarcheazaProps) {
  const da = Math.round(btts.procent);
  const nu = 100 - da;

  return (
    <Card className="h-full p-5">
      <h2 className="text-base font-bold text-ink">Ambele echipe marchează</h2>
      <p className="mt-0.5 text-sm text-ink2">Probabilitate</p>

      <div className="mt-4 flex justify-around gap-2">
        <div className="text-center">
          <p className="text-xs text-ink2">Da</p>
          <p className="mt-1 text-base font-extrabold text-primary sm:text-lg">{da}%</p>
        </div>
        <div className="text-center">
          <p className="text-xs text-ink2">Nu</p>
          <p className="mt-1 text-base font-extrabold text-accent sm:text-lg">{nu}%</p>
        </div>
      </div>
    </Card>
  );
}
