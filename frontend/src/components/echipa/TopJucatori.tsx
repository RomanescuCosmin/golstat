import type { JucatorStat, TopJucatori as TopJucatoriData } from '../../api/types';
import { Card } from '../ui/Card';
import { IconUser } from '../ui/icons';

interface CardJucatorProps {
  titlu: string;
  jucator: JucatorStat;
  unitate: string;
}

function CardJucator({ titlu, jucator, unitate }: CardJucatorProps) {
  return (
    <Card className="flex items-center gap-3 p-4">
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
      <div className="min-w-0">
        <p className="text-[11px] font-semibold uppercase tracking-wide text-ink2">{titlu}</p>
        <p className="truncate text-sm font-bold text-ink">{jucator.nume ?? '—'}</p>
        <p className="text-ink">
          <span className="text-lg font-extrabold tabular-nums">{jucator.valoare}</span>
          <span className="ml-1 text-xs text-ink2">{unitate}</span>
        </p>
      </div>
    </Card>
  );
}

interface TopJucatoriProps {
  top: TopJucatoriData | null;
}

/** Rand de pana la 4 carduri: golgheter, pasator, cel mai utilizat, cartonase; sare peste cei null. */
export function TopJucatori({ top }: TopJucatoriProps) {
  if (!top) return null;

  const carduri: CardJucatorProps[] = [];
  if (top.golgheter) carduri.push({ titlu: 'Golgheter', jucator: top.golgheter, unitate: 'goluri' });
  if (top.pasator) carduri.push({ titlu: 'Pase decisive', jucator: top.pasator, unitate: 'pase' });
  if (top.minute) carduri.push({ titlu: 'Cel mai utilizat', jucator: top.minute, unitate: 'min. jucate' });
  if (top.cartonase) carduri.push({ titlu: 'Cartonașe', jucator: top.cartonase, unitate: 'cartonașe' });

  if (carduri.length === 0) return null;

  return (
    <div>
      <h2 className="mb-3 text-base font-bold text-ink">Jucători de urmărit</h2>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {carduri.map((c) => (
          <CardJucator key={c.titlu} {...c} />
        ))}
      </div>
    </div>
  );
}
