import type { IntalnireDirectaDto } from '../../api/types';
import { formatDataScurta } from '../../lib/format';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { TeamLogo } from '../ui/TeamLogo';

interface IntalniriDirecteProps {
  intalniri: IntalnireDirectaDto[];
}

/** Ultimele 5 intalniri directe: logo-uri mici, scor si data. */
export function IntalniriDirecte({ intalniri }: IntalniriDirecteProps) {
  const ultimele = intalniri.slice(0, 5);

  return (
    <Card className="h-full p-5">
      <h2 className="text-base font-bold text-ink">
        Întâlniri directe <span className="font-normal text-ink2">(ultimele 5 meciuri)</span>
      </h2>

      {ultimele.length === 0 ? (
        <EmptyState titlu="Fără întâlniri directe" mesaj="Echipele nu s-au mai întâlnit recent." />
      ) : (
        <div className="mt-4 flex flex-wrap justify-between gap-x-4 gap-y-5">
          {ultimele.map((intalnire) => (
            <div key={intalnire.fixtureId} className="flex min-w-[4.5rem] flex-col items-center gap-1.5">
              <div className="flex items-center gap-1.5">
                <TeamLogo nume={intalnire.gazde.nume} logo={intalnire.gazde.logo} size={22} />
                <TeamLogo nume={intalnire.oaspeti.nume} logo={intalnire.oaspeti.logo} size={22} />
              </div>
              <span className="text-sm font-bold tabular-nums text-ink">
                {intalnire.golGazde ?? '—'}-{intalnire.golOaspeti ?? '—'}
              </span>
              <span className="text-xs text-ink2">{formatDataScurta(intalnire.data)}</span>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
