import { useEffect, useRef, type ReactNode } from 'react';
import { deruleazaLin } from '../../lib/derulare';
import { LIGI } from '../../lib/ligi';
import { LigaLogo } from '../ui/LigaLogo';
import { IconChevronLeft, IconChevronRight, IconGlobe } from '../ui/icons';

interface CaruselCompetitiiProps {
  /** Competitia selectata; `null` = toate. */
  selectata: number | null;
  onAlege: (leagueId: number | null) => void;
}

/**
 * Toate competitiile ca pastile intr-un carusel orizontal cu derularea lina a caruselului live;
 * inlocuieste dropdownul de competitii pe pagina Meciuri. Pastila activa e adusa in centru.
 */
export function CaruselCompetitii({ selectata, onAlege }: CaruselCompetitiiProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el || selectata == null) return;
    const pastila = el.querySelector<HTMLElement>(`[data-liga="${selectata}"]`);
    if (!pastila) return;
    const tinta = pastila.offsetLeft - (el.clientWidth - pastila.offsetWidth) / 2;
    deruleazaLin(el, tinta - el.scrollLeft);
  }, [selectata]);

  const sare = (directie: number) => {
    const el = scrollRef.current;
    if (el) deruleazaLin(el, directie * Math.max(240, el.clientWidth * 0.7));
  };

  return (
    <div className="relative">
      <div
        ref={scrollRef}
        className="relative flex gap-2 overflow-x-auto px-1 py-1 [mask-image:linear-gradient(90deg,transparent,black_36px,black_calc(100%-36px),transparent)] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        <Pastila activa={selectata == null} onClick={() => onAlege(null)} icon={<IconGlobe width={16} height={16} />}>
          Toate competițiile
        </Pastila>
        {LIGI.map((liga) => (
          <Pastila
            key={liga.id}
            dataLiga={liga.id}
            activa={selectata === liga.id}
            onClick={() => onAlege(selectata === liga.id ? null : liga.id)}
            icon={<LigaLogo id={liga.id} nume={liga.nume} size={16} />}
            title={liga.regiune}
          >
            {liga.nume}
          </Pastila>
        ))}
      </div>

      <button
        type="button"
        aria-label="Derulează stânga"
        onClick={() => sare(-1)}
        className="absolute -left-1 top-1/2 z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-line bg-card/90 text-ink2 shadow-card backdrop-blur transition hover:scale-105 hover:border-primary/40 hover:text-ink"
      >
        <IconChevronLeft width={15} height={15} />
      </button>
      <button
        type="button"
        aria-label="Derulează dreapta"
        onClick={() => sare(1)}
        className="absolute -right-1 top-1/2 z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-line bg-card/90 text-ink2 shadow-card backdrop-blur transition hover:scale-105 hover:border-primary/40 hover:text-ink"
      >
        <IconChevronRight width={15} height={15} />
      </button>
    </div>
  );
}

function Pastila({
  activa,
  onClick,
  icon,
  children,
  title,
  dataLiga,
}: {
  activa: boolean;
  onClick: () => void;
  icon: ReactNode;
  children: ReactNode;
  title?: string;
  dataLiga?: number;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activa}
      title={title}
      data-liga={dataLiga}
      className={`flex h-9 shrink-0 items-center gap-2 rounded-full border px-3.5 text-[13px] font-semibold transition ${
        activa
          ? 'border-primary bg-primary text-white shadow-[0_4px_14px_rgb(var(--gs-primary)/0.3)]'
          : 'border-line bg-card text-ink2 hover:border-primary/40 hover:text-ink'
      }`}
    >
      {icon}
      <span className="whitespace-nowrap">{children}</span>
    </button>
  );
}
