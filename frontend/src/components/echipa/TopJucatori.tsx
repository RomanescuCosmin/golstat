import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { JucatorStat, TopJucatori as TopJucatoriData } from '../../api/types';
import { useCountUp } from '../../hooks/useCountUp';
import { Card } from '../ui/Card';
import { IconBall, IconCartonas, IconCeas, IconChart, IconUser } from '../ui/icons';

const nrIntreg = new Intl.NumberFormat('ro-RO');

interface CardJucatorProps {
  titlu: string;
  jucator: JucatorStat;
  unitate: string;
  icon: ReactNode;
  /** Titlu colorat (ex. roșu la cartonașe roșii), ca în design. */
  titluClass?: string;
}

function CardJucator({ titlu, jucator, unitate, icon, titluClass = 'text-ink2' }: CardJucatorProps) {
  const valoare = useCountUp(jucator.valoare);
  const continut = (
    <Card className="h-full p-4 transition duration-200 hover:-translate-y-0.5 hover:shadow-cardHover">
      <p className={`text-[11px] font-semibold uppercase tracking-wide ${titluClass}`}>{titlu}</p>
      <div className="mt-3 flex items-center gap-3">
        {jucator.foto ? (
          <img
            src={jucator.foto}
            alt={jucator.nume ?? ''}
            width={44}
            height={44}
            loading="lazy"
            className="h-11 w-11 shrink-0 rounded-full object-cover"
          />
        ) : (
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary dark:bg-primary/20">
            <IconUser width={22} height={22} />
          </span>
        )}
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-bold text-ink">{jucator.nume ?? '—'}</p>
          <p className="text-xl font-extrabold leading-tight tabular-nums text-ink">{nrIntreg.format(valoare)}</p>
          <p className="text-xs text-ink2">{unitate}</p>
        </div>
        <span className="shrink-0 self-center">{icon}</span>
      </div>
    </Card>
  );
  return jucator.playerId != null ? (
    <Link to={`/jucator/${jucator.playerId}`} className="block">
      {continut}
    </Link>
  ) : (
    continut
  );
}

interface TopJucatoriProps {
  top: TopJucatoriData | null;
}

/** Cinci carduri de jucători: golgheter, pase decisive, cel mai utilizat, cartonașe galbene / roșii. */
export function TopJucatori({ top }: TopJucatoriProps) {
  if (!top) return null;

  const carduri: CardJucatorProps[] = [];
  if (top.golgheter)
    carduri.push({ titlu: 'Golgheter', jucator: top.golgheter, unitate: 'Goluri', icon: <IconBall width={18} height={18} className="text-primary" /> });
  if (top.pasator)
    carduri.push({ titlu: 'Pase decisive', jucator: top.pasator, unitate: 'Pase decisive', icon: <IconChart width={18} height={18} className="text-primary" /> });
  if (top.minute)
    carduri.push({ titlu: 'Cel mai utilizat', jucator: top.minute, unitate: 'Minute jucate', icon: <IconCeas width={18} height={18} className="text-primary" /> });
  if (top.galbene)
    carduri.push({ titlu: 'Cartonașe galbene', jucator: top.galbene, unitate: 'Cartonașe', icon: <IconCartonas width={18} height={18} className="text-[#F59E0B]" /> });
  if (top.rosii)
    carduri.push({
      titlu: 'Cartonașe roșii',
      jucator: top.rosii,
      unitate: 'Cartonașe',
      icon: <IconCartonas width={18} height={18} className="text-accent" />,
      titluClass: 'text-accent',
    });

  if (carduri.length === 0) return null;

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      {carduri.map((c) => (
        <CardJucator key={c.titlu} {...c} />
      ))}
    </div>
  );
}
