import { useNavigate } from 'react-router-dom';
import type { MeciForma } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { IconChevronRight } from '../ui/icons';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';

const variantaRezultat = { V: 'win', E: 'draw', I: 'loss' } as const;

/** "Regular Season - 35" → "Et. 35"; altfel textul brut. */
function etapaScurta(runda: string | null): string | null {
  if (!runda) return null;
  const m = runda.match(/(\d+)\s*$/);
  return m ? `Et. ${m[1]}` : runda;
}

/** Locul echipei noastre in rand (nu avem numele propriu in MeciForma). */
function EchipaNoastra({ eticheta }: { eticheta: string }) {
  return <span className="truncate text-xs font-medium text-ink2/70">{eticheta}</span>;
}

/** Lista rezultatelor recente: competiție + etapă, gazde – scor – oaspeți, badge V/E/I; rând → centrul meciului. */
export function RezultateRecente({ rezultate }: { rezultate: MeciForma[] }) {
  const navigate = useNavigate();

  return (
    <Card>
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-sm font-extrabold text-ink">Rezultate recente</h2>
      </div>
      {rezultate.length === 0 ? (
        <EmptyState titlu="Fără rezultate" mesaj="Nu există meciuri recente pentru această echipă." />
      ) : (
        <>
          <div className="max-h-[26rem] divide-y divide-line overflow-y-auto">
            {rezultate.map((m) => {
              const golAcasa = m.acasa ? m.golMarcate : m.golPrimite;
              const golDeplasare = m.acasa ? m.golPrimite : m.golMarcate;
              return (
                <button
                  key={m.fixtureId}
                  type="button"
                  onClick={() => navigate(`/meci/${m.fixtureId}/centru`)}
                  className="flex w-full items-center gap-2.5 px-4 py-3 text-left transition duration-200 hover:bg-bg focus:bg-bg focus:outline-none"
                >
                  <span className="flex w-10 shrink-0 flex-col items-center gap-0.5">
                    <LigaLogo logo={m.ligaLogo} nume={m.liga} size={18} />
                    <span className="whitespace-nowrap text-[10px] font-medium leading-none text-ink2/80">
                      {etapaScurta(m.runda) ?? formatDataScurta(m.data)}
                    </span>
                  </span>

                  <span className="flex min-w-0 flex-1 items-center justify-end gap-2">
                    {m.acasa ? (
                      <EchipaNoastra eticheta="Acasă" />
                    ) : (
                      <>
                        <span className="truncate text-sm font-semibold text-ink">{numeEchipa(m.adversar)}</span>
                        <TeamLogo nume={m.adversar.nume} logo={m.adversar.logo} size={20} />
                      </>
                    )}
                  </span>

                  <span className="w-12 shrink-0 text-center text-sm font-bold tabular-nums text-ink">
                    {golAcasa ?? '—'} - {golDeplasare ?? '—'}
                  </span>

                  <span className="flex min-w-0 flex-1 items-center gap-2">
                    {m.acasa ? (
                      <>
                        <TeamLogo nume={m.adversar.nume} logo={m.adversar.logo} size={20} />
                        <span className="truncate text-sm font-semibold text-ink">{numeEchipa(m.adversar)}</span>
                      </>
                    ) : (
                      <EchipaNoastra eticheta="Deplasare" />
                    )}
                  </span>

                  <Badge variant={variantaRezultat[m.rezultat]} className="h-6 w-6 shrink-0 justify-center rounded px-0">
                    {m.rezultat}
                  </Badge>
                </button>
              );
            })}
          </div>
          <div className="border-t border-line">
            <button
              type="button"
              title="În curând"
              className="flex w-full items-center justify-center gap-1 rounded-b-card px-5 py-3 text-sm font-semibold text-primary transition duration-200 hover:bg-bg"
            >
              Vezi toate rezultatele
              <IconChevronRight width={16} height={16} />
            </button>
          </div>
        </>
      )}
    </Card>
  );
}
