import type { AntetEchipa as AntetEchipaData, MeciForma, SumarSezon } from '../../api/types';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { TeamLogo } from '../ui/TeamLogo';
import { IconStar } from '../ui/icons';
import { FormaRecenta } from './FormaRecenta';

interface CifraProps {
  eticheta: string;
  valoare: number | null;
}

function Cifra({ eticheta, valoare }: CifraProps) {
  return (
    <div className="flex flex-col items-center">
      <span className="text-2xl font-extrabold tabular-nums text-ink">{valoare ?? '—'}</span>
      <span className="text-[11px] font-medium uppercase tracking-wide text-ink2">{eticheta}</span>
    </div>
  );
}

interface AntetEchipaProps {
  antet: AntetEchipaData;
  sumar: SumarSezon | null;
  forma: MeciForma[];
}

/** Cardul-antet al paginii echipei: emblema + identitate la stanga, cifrele sezonului + forma la dreapta. */
export function AntetEchipa({ antet, sumar, forma }: AntetEchipaProps) {
  const detalii: string[] = [];
  if (antet.antrenor) detalii.push(`Antrenor: ${antet.antrenor}`);
  if (antet.stadion) detalii.push(`Stadion: ${antet.stadion}`);
  if (antet.capacitate != null) detalii.push(`Capacitate: ${antet.capacitate.toLocaleString('ro-RO')}`);

  return (
    <Card className="p-5 sm:p-6">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-center gap-4">
          <TeamLogo nume={antet.nume} logo={antet.logo} size={64} />
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="truncate text-xl font-extrabold text-ink sm:text-2xl">
                {antet.nume ?? `Echipa #${antet.teamId}`}
              </h1>
              <IconStar width={18} height={18} className="shrink-0 text-ink2/40" />
            </div>
            <div className="flex items-center gap-1.5 text-sm font-medium text-ink2">
              {antet.liga && (
                <LigaLogo id={antet.leagueId ?? undefined} logo={antet.ligaLogo} nume={antet.liga} size={16} />
              )}
              <span>{[antet.tara, antet.liga].filter(Boolean).join(' • ')}</span>
            </div>
            {detalii.length > 0 && (
              <div className="mt-2 space-y-0.5">
                {detalii.map((linie) => (
                  <p key={linie} className="text-xs text-ink2">
                    {linie}
                  </p>
                ))}
              </div>
            )}
          </div>
        </div>

        {sumar && (
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-5 sm:gap-7">
              <Cifra eticheta="Poziție" valoare={sumar.pozitie} />
              <Cifra eticheta="Puncte" valoare={sumar.puncte} />
              <Cifra eticheta="Victorii" valoare={sumar.victorii} />
              <Cifra eticheta="Egaluri" valoare={sumar.egaluri} />
              <Cifra eticheta="Înfrângeri" valoare={sumar.infrangeri} />
            </div>
            <div className="flex items-center gap-2 lg:justify-end">
              <span className="text-[11px] font-semibold uppercase tracking-wide text-ink2">Formă</span>
              <FormaRecenta forma={forma} />
            </div>
          </div>
        )}
      </div>
    </Card>
  );
}
