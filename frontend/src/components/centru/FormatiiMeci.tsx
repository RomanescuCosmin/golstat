import type { EchipaDto, Formatii } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { FormatieBadge } from '../lineup/FormatieBadge';
import { ListaRezerve } from '../lineup/ListaRezerve';
import { Teren } from '../lineup/Teren';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

interface FormatiiMeciProps {
  formatii: Formatii;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
}

/** Formatiile meciului: teren cu titularii + antrenor + rezerve, refolosind componentele de lineup. */
export function FormatiiMeci({ formatii, gazde, oaspeti }: FormatiiMeciProps) {
  const numeGazde = numeEchipa(gazde);
  const numeOaspeti = numeEchipa(oaspeti);
  const areTitulari = formatii.gazde.titulari.length > 0 || formatii.oaspeti.titulari.length > 0;

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Formații</h2>

      {!areTitulari ? (
        <EmptyState titlu="Fără formații" mesaj="Formațiile de start nu au fost anunțate." />
      ) : (
        <>
          <div className="mt-4 flex items-start justify-between gap-3">
            <div>
              <FormatieBadge nume={numeGazde} formatie={formatii.gazde.formatie} aliniere="stanga" />
              {formatii.gazde.antrenor && <p className="mt-0.5 text-xs text-ink2">Antrenor: {formatii.gazde.antrenor}</p>}
            </div>
            <div className="text-right">
              <FormatieBadge nume={numeOaspeti} formatie={formatii.oaspeti.formatie} aliniere="dreapta" />
              {formatii.oaspeti.antrenor && <p className="mt-0.5 text-xs text-ink2">Antrenor: {formatii.oaspeti.antrenor}</p>}
            </div>
          </div>

          <div className="mt-3">
            <Teren gazde={formatii.gazde.titulari} oaspeti={formatii.oaspeti.titulari} />
          </div>
        </>
      )}

      <div className="mt-5 grid gap-5 sm:grid-cols-2">
        <ListaRezerve titlu={`Rezerve · ${numeGazde}`} rezerve={formatii.gazde.rezerve} />
        <ListaRezerve titlu={`Rezerve · ${numeOaspeti}`} rezerve={formatii.oaspeti.rezerve} />
      </div>
    </Card>
  );
}
