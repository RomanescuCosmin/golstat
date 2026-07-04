import type { ReactNode } from 'react';
import type { EvenimentMeci } from '../../api/types';
import { Card } from '../ui/Card';
import { IconBall, IconCartonas, IconSchimbare, IconVar } from '../ui/icons';

interface CronologieMeciProps {
  evenimente: EvenimentMeci[];
}

function minutText(ev: EvenimentMeci): string {
  const baza = ev.minut != null ? `${ev.minut}` : '';
  const extra = ev.minutExtra != null ? `+${ev.minutExtra}` : '';
  return `${baza}${extra}'`;
}

function iconEveniment(ev: EvenimentMeci): ReactNode {
  switch (ev.tip) {
    case 'Goal':
      return <IconBall width={18} height={18} className="text-win" />;
    case 'Card': {
      const rosu = (ev.detaliu ?? '').toLowerCase().includes('red');
      return rosu ? (
        <IconCartonas width={18} height={18} className="text-accent" />
      ) : (
        <IconCartonas width={18} height={18} style={{ color: '#E3B23B' }} />
      );
    }
    case 'subst':
      return <IconSchimbare width={18} height={18} className="text-ink2" />;
    case 'Var':
      return <IconVar width={18} height={18} className="text-ink2" />;
    default:
      return <IconBall width={18} height={18} className="text-ink2" />;
  }
}

function descriere(ev: EvenimentMeci): { titlu: string | null; sub: string | null } {
  switch (ev.tip) {
    case 'Goal':
      return { titlu: ev.jucator, sub: ev.asist ? `pasă: ${ev.asist}` : ev.detaliu };
    case 'subst':
      return { titlu: ev.jucator, sub: ev.detaliu ? `iese: ${ev.detaliu}` : null };
    case 'Var':
      return { titlu: ev.detaliu, sub: null };
    default:
      return { titlu: ev.jucator, sub: ev.detaliu };
  }
}

function Continut({ ev, spreDreapta }: { ev: EvenimentMeci; spreDreapta: boolean }) {
  const { titlu, sub } = descriere(ev);
  return (
    <div className={`flex min-w-0 items-start gap-2 ${spreDreapta ? 'flex-row-reverse text-right' : 'text-left'}`}>
      <span className="mt-0.5 shrink-0">{iconEveniment(ev)}</span>
      <span className="min-w-0">
        {titlu && <span className="block break-words text-sm font-semibold text-ink">{titlu}</span>}
        {sub && <span className="block break-words text-xs text-ink2">{sub}</span>}
      </span>
    </div>
  );
}

/** Cronologia meciului: evenimentele pe axa verticala, gazdele la stanga si oaspetii la dreapta. */
export function CronologieMeci({ evenimente }: CronologieMeciProps) {
  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Cronologie</h2>

      <div className="relative mt-4">
        <div className="absolute inset-y-0 left-1/2 w-px -translate-x-1/2 bg-line" aria-hidden />
        <ul className="relative space-y-4">
          {evenimente.map((ev, i) => (
            <li
              key={ev.id ?? i}
              className="grid grid-cols-[1fr_auto_1fr] items-center gap-2 sm:gap-3"
            >
              <div className="min-w-0">{ev.gazde && <Continut ev={ev} spreDreapta />}</div>
              <span className="rounded-full border border-line bg-bg px-2 py-0.5 text-xs font-semibold tabular-nums text-ink2">
                {minutText(ev)}
              </span>
              <div className="min-w-0">{!ev.gazde && <Continut ev={ev} spreDreapta={false} />}</div>
            </li>
          ))}
        </ul>
      </div>
    </Card>
  );
}
