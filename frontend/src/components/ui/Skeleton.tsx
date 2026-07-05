import { Card } from './Card';

/** Placeholder shimmer de baza; dimensionat de apelant prin className. */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden className={`animate-pulse rounded-md bg-ink2/10 dark:bg-ink2/15 ${className}`} />;
}

/** Un rand generic de lista: avatar rotund + doua linii de text + valoare in dreapta. */
export function SkeletonRand() {
  return (
    <div className="flex items-center gap-3 px-5 py-3">
      <Skeleton className="h-8 w-8 shrink-0 rounded-full" />
      <div className="min-w-0 flex-1 space-y-1.5">
        <Skeleton className="h-3.5 w-2/5" />
        <Skeleton className="h-3 w-1/4" />
      </div>
      <Skeleton className="h-5 w-8 shrink-0" />
    </div>
  );
}

/** Card cu bara de titlu + un numar de randuri skeleton (liste, topuri, competitii). */
export function SkeletonCard({ randuri = 5 }: { randuri?: number }) {
  return (
    <Card>
      <div className="border-b border-line px-5 py-4">
        <Skeleton className="h-4 w-40" />
      </div>
      <div className="divide-y divide-line">
        {Array.from({ length: randuri }, (_, i) => (
          <SkeletonRand key={i} />
        ))}
      </div>
    </Card>
  );
}
