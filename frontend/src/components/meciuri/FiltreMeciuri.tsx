import type { ReactNode } from 'react';
import { BandaZile } from './BandaZile';
import { SelectorLiga, type OptiuneLiga } from './SelectorLiga';
import { IconCeas, IconFiltru, IconLive, IconStar } from '../ui/icons';

export interface StareFiltre {
  live: boolean;
  favorite: boolean;
  curand: boolean;
}

interface FiltreMeciuriProps {
  dataISO: string;
  onData: (dataISO: string) => void;
  liga: number | null;
  onLiga: (leagueId: number | null) => void;
  optiuniLigi: OptiuneLiga[];
  filtre: StareFiltre;
  onFiltre: (filtre: StareFiltre) => void;
}

/**
 * Bara de filtre a paginii Meciuri: selector de zi + competitie, buton „Filtre" (reset) si
 * comutatoare Live / Favorite / Începe curând. Stil pastila, doar accente albastru/rosu.
 */
export function FiltreMeciuri({ dataISO, onData, liga, onLiga, optiuniLigi, filtre, onFiltre }: FiltreMeciuriProps) {
  const active = filtre.live || filtre.favorite || filtre.curand;

  return (
    <div className="mb-5 flex flex-wrap items-center gap-2.5">
      <BandaZile selectata={dataISO} onSelect={onData} />

      <div className="min-w-[12rem]">
        <SelectorLiga leagueId={liga} onChange={onLiga} optiuni={optiuniLigi} />
      </div>

      <div className="ml-auto flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => onFiltre({ live: false, favorite: false, curand: false })}
          disabled={!active}
          className={`inline-flex h-10 items-center gap-1.5 rounded-full border px-3.5 text-sm font-semibold transition ${
            active
              ? 'border-line text-ink hover:bg-bg'
              : 'cursor-default border-line text-ink2/60'
          }`}
          title={active ? 'Resetează filtrele' : 'Filtre'}
        >
          <IconFiltru width={16} height={16} />
          Filtre
          {active && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
        </button>

        <Toggle
          activ={filtre.live}
          onClick={() => onFiltre({ ...filtre, live: !filtre.live })}
          icon={<IconLive width={15} height={15} />}
          culoareActiv="accent"
        >
          Live
        </Toggle>
        <Toggle
          activ={filtre.favorite}
          onClick={() => onFiltre({ ...filtre, favorite: !filtre.favorite })}
          icon={<IconStar width={15} height={15} fill={filtre.favorite ? 'currentColor' : 'none'} />}
        >
          Favorite
        </Toggle>
        <Toggle
          activ={filtre.curand}
          onClick={() => onFiltre({ ...filtre, curand: !filtre.curand })}
          icon={<IconCeas width={15} height={15} />}
        >
          Începe curând
        </Toggle>
      </div>
    </div>
  );
}

function Toggle({
  activ,
  onClick,
  icon,
  children,
  culoareActiv = 'primary',
}: {
  activ: boolean;
  onClick: () => void;
  icon: ReactNode;
  children: ReactNode;
  culoareActiv?: 'primary' | 'accent';
}) {
  const activClasa = culoareActiv === 'accent' ? 'border-accent bg-accent text-white' : 'border-primary bg-primary text-white';
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activ}
      className={`inline-flex h-10 items-center gap-1.5 rounded-full border px-3.5 text-sm font-semibold transition ${
        activ ? activClasa : 'border-line text-ink2 hover:bg-bg hover:text-ink'
      }`}
    >
      {icon}
      {children}
    </button>
  );
}
