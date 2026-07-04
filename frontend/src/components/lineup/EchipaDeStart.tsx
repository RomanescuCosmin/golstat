import type { EchipaDeStartDto, EchipaDto } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { IconFluier } from '../ui/icons';
import { FormatieBadge } from './FormatieBadge';
import { ListaIndisponibili } from './ListaIndisponibili';
import { ListaRezerve } from './ListaRezerve';
import { Teren } from './Teren';

interface EchipaDeStartProps {
  echipe: EchipaDeStartDto | null;
  /** Nume optional pentru etichete (DTO-ul de formatii nu contine numele echipei). */
  gazde?: EchipaDto;
  oaspeti?: EchipaDto;
}

/** "Echipă de start": teren cu titularii + rezerve, indisponibili si arbitrul meciului. */
export function EchipaDeStart({ echipe, gazde, oaspeti }: EchipaDeStartProps) {
  if (!echipe) return null;

  const numeGazde = gazde?.nume ?? 'Gazde';
  const numeOaspeti = oaspeti?.nume ?? 'Oaspeți';

  const areTitulari = echipe.gazde.titulari.length > 0 || echipe.oaspeti.titulari.length > 0;

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Echipă de start</h2>

      {!areTitulari ? (
        <EmptyState titlu="Fără formații" mesaj="Formațiile de start nu au fost încă anunțate." />
      ) : (
        <>
          <div className="mt-4 flex items-center justify-between gap-3">
            <FormatieBadge nume={numeGazde} formatie={echipe.gazde.formatie} aliniere="stanga" />
            <FormatieBadge nume={numeOaspeti} formatie={echipe.oaspeti.formatie} aliniere="dreapta" />
          </div>

          <div className="mt-3">
            <Teren gazde={echipe.gazde.titulari} oaspeti={echipe.oaspeti.titulari} />
          </div>
        </>
      )}

      <div className="mt-5 grid gap-5 sm:grid-cols-2">
        <ListaRezerve titlu={`Rezerve · ${numeGazde}`} rezerve={echipe.gazde.rezerve} />
        <ListaRezerve titlu={`Rezerve · ${numeOaspeti}`} rezerve={echipe.oaspeti.rezerve} />
        <ListaIndisponibili titlu={`Indisponibili · ${numeGazde}`} indisponibili={echipe.gazde.indisponibili} />
        <ListaIndisponibili titlu={`Indisponibili · ${numeOaspeti}`} indisponibili={echipe.oaspeti.indisponibili} />
      </div>

      {echipe.arbitru && (
        <div className="mt-5 flex items-center gap-2 border-t border-line pt-4 text-sm text-ink2">
          <IconFluier width={18} height={18} className="shrink-0 text-ink2" />
          <span className="font-semibold uppercase tracking-wide text-[11px]">Informații meci</span>
          <span className="text-ink">Arbitru: {echipe.arbitru}</span>
        </div>
      )}
    </Card>
  );
}
