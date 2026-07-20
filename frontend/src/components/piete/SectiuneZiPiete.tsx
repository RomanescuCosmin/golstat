import { formatData } from '../../lib/format';
import { etichetaZi, type ZiFiltrata } from '../../lib/piete';
import { Card } from '../ui/Card';
import { RandPiataMeci } from './RandPiataMeci';

/** O zi din listă: antetul zilei + meciurile ei, deja sortate descrescător. */
export function SectiuneZiPiete({ zi }: { zi: ZiFiltrata }) {
  const scurt = etichetaZi(zi.data);
  return (
    <section>
      <h2 className="mb-2 flex items-baseline gap-2 text-sm font-bold text-ink">
        {scurt && <span className="text-primary">{scurt}</span>}
        <span className={scurt ? 'text-xs font-semibold text-ink2' : undefined}>
          {formatData(zi.data)}
        </span>
        <span className="ml-auto text-xs font-semibold text-ink2">
          {zi.randuri.length} {zi.randuri.length === 1 ? 'meci' : 'meciuri'}
        </span>
      </h2>
      <Card className="overflow-hidden p-0">
        {zi.randuri.map((rand) => (
          <RandPiataMeci key={rand.meci.fixtureId} rand={rand} />
        ))}
      </Card>
    </section>
  );
}
