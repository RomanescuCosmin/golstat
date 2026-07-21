import { formatData } from '../../lib/format';
import { etichetaZi, type ZiFiltrata } from '../../lib/piete';
import { RandPiataMeci } from './RandPiataMeci';

interface Props {
  zi: ZiFiltrata;
  /** Eticheta lungă a pieței alese, aceeași pentru toate rândurile zilei. */
  piata: string;
  favorite: number[];
  onFavorit: (fixtureId: number) => void;
}

/**
 * O zi din listă: antetul zilei, despărțit de meciuri printr-o linie fină, apoi rândurile deja
 * sortate descrescător. Fiecare meci e un card de sine stătător, ca ochiul să prindă limitele
 * rândului fără să urmărească o coloană de separatoare.
 */
export function SectiuneZiPiete({ zi, piata, favorite, onFavorit }: Props) {
  const scurt = etichetaZi(zi.data);
  return (
    <section>
      <div className="mb-3 flex items-baseline gap-2 border-b border-line pb-2.5">
        <h2 className="text-sm font-bold text-ink">{scurt ?? formatData(zi.data)}</h2>
        {scurt && <span className="text-xs font-semibold text-ink2">{formatData(zi.data)}</span>}
        <span className="ml-auto text-xs font-semibold tabular-nums text-ink2">
          {zi.randuri.length} {zi.randuri.length === 1 ? 'meci' : 'meciuri'}
        </span>
      </div>
      <div className="flex flex-col gap-2">
        {zi.randuri.map((rand) => (
          <RandPiataMeci
            key={rand.meci.fixtureId}
            rand={rand}
            piata={piata}
            favorit={favorite.includes(rand.meci.fixtureId)}
            onFavorit={onFavorit}
          />
        ))}
      </div>
    </section>
  );
}
