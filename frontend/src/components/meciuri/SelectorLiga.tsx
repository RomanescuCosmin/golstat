import { Dropdown } from '../ui/Dropdown';
import { LigaLogo } from '../ui/LigaLogo';
import { IconGlobe } from '../ui/icons';

/** O optiune de competitie in selector. */
export interface OptiuneLiga {
  id: number;
  nume: string;
}

interface SelectorLigaProps {
  /** Liga selectata; `null` = toate competitiile. */
  leagueId: number | null;
  onChange: (leagueId: number | null) => void;
  /** Competitiile disponibile (dinamic, din ziua incarcata). Prima optiune e mereu „Toate competițiile". */
  optiuni: OptiuneLiga[];
}

/** Valoarea interna pentru „Toate competițiile" (niciun leagueId real nu e negativ). */
const TOATE = -1;

/** Dropdown de competitii, cu logo-ul fiecarei ligi; optiunile vin dinamic, deci reflecta exact ce e afisat. */
export function SelectorLiga({ leagueId, onChange, optiuni }: SelectorLigaProps) {
  return (
    <Dropdown
      eticheta="Competiție"
      varianta="camp"
      dreapta
      valoare={leagueId ?? TOATE}
      onSchimba={(v) => onChange(v === TOATE ? null : v)}
      optiuni={[
        { valoare: TOATE, eticheta: 'Toate competițiile', icon: <IconGlobe width={16} height={16} /> },
        ...optiuni.map((l) => ({
          valoare: l.id,
          eticheta: l.nume,
          icon: <LigaLogo id={l.id} nume={l.nume} size={18} />,
        })),
      ]}
    />
  );
}
