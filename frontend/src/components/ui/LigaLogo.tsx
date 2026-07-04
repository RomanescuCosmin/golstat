import { useState } from 'react';
import { ID_CUPA_MONDIALA, idLigaEfectiv, esteLogoPlaceholder, logoLiga } from '../../lib/ligi';
import { IconTrophy } from './icons';
import { WorldCupLogo } from './WorldCupLogo';

interface LigaLogoProps {
  /** Id-ul ligii (deriva URL-ul de pe CDN) SAU un logo explicit (ex. din backend). */
  id?: number;
  logo?: string | null;
  nume?: string | null;
  size?: number;
  className?: string;
}

/**
 * Logo de competitie cu fallback pe trofeu: daca imaginea pica, lipseste, SAU sursa are doar
 * placeholder-ul gri al API-Football (ex. Campionatul Mondial), afisam trofeul.
 */
export function LigaLogo({ id, logo, nume, size = 22, className = '' }: LigaLogoProps) {
  const [failed, setFailed] = useState(false);

  // Campionatul Mondial: sursa are doar placeholder gri → folosim logoul propriu (trofeu auriu cu glob).
  if (idLigaEfectiv(id, logo) === ID_CUPA_MONDIALA) {
    return <WorldCupLogo size={size} className={className} />;
  }

  const placeholder = esteLogoPlaceholder(id, logo);
  const src = placeholder ? null : (logo ?? (id != null ? logoLiga(id) : null));

  if (!src || failed) {
    return <IconTrophy width={size} height={size} className={`shrink-0 ${className}`} />;
  }

  return (
    <img
      src={src}
      alt={nume ?? 'Competiție'}
      width={size}
      height={size}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`shrink-0 object-contain ${className}`}
    />
  );
}
