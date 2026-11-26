import { Dropdown } from '../ui/Dropdown';

interface SelectorSezonProps {
  sezoane: number[];
  valoare: number | null;
  onChange: (sezon: number) => void;
}

/** Formatul "2023/24" dintr-un an de start de sezon. */
function etichetaSezon(an: number): string {
  const urm = String((an + 1) % 100).padStart(2, '0');
  return `${an}/${urm}`;
}

/** Dropdown de sezon (pagina echipei, competitiei); re-fetch la schimbare. */
export function SelectorSezon({ sezoane, valoare, onChange }: SelectorSezonProps) {
  if (sezoane.length === 0) {
    return null;
  }
  return (
    <Dropdown
      eticheta="Sezon"
      dreapta
      valoare={valoare ?? sezoane[0]!}
      onSchimba={onChange}
      optiuni={sezoane.map((an) => ({ valoare: an, eticheta: etichetaSezon(an) }))}
    />
  );
}
