import type { ReactNode } from 'react';

export interface Tab {
  id: string;
  eticheta: string;
  icon?: ReactNode;
}

interface TaburiProps {
  taburi: Tab[];
  activ: string;
  onSchimba: (id: string) => void;
  /** `linie` = underline la nivel de pagina; `pilule` = pastile pentru sub-secțiuni. */
  varianta?: 'linie' | 'pilule';
  className?: string;
}

/** Taburi interactive reutilizabile, cu două stiluri: underline (pagină) și pastile (sub-secțiuni). */
export function Taburi({ taburi, activ, onSchimba, varianta = 'linie', className = '' }: TaburiProps) {
  if (varianta === 'pilule') {
    return (
      <div className={`flex gap-2 overflow-x-auto pb-1 ${className}`} role="tablist">
        {taburi.map((tab) => {
          const selectat = tab.id === activ;
          return (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={selectat}
              onClick={() => onSchimba(tab.id)}
              className={`flex shrink-0 items-center gap-2 whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition duration-200 ${
                selectat
                  ? 'bg-primary text-white shadow-card'
                  : 'bg-ink2/10 text-ink2 hover:bg-ink2/20 hover:text-ink dark:bg-ink2/15'
              }`}
            >
              {tab.icon}
              {tab.eticheta}
            </button>
          );
        })}
      </div>
    );
  }

  return (
    <div className={`flex gap-6 overflow-x-auto border-b border-line sm:gap-8 ${className}`} role="tablist">
      {taburi.map((tab) => {
        const selectat = tab.id === activ;
        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={selectat}
            onClick={() => onSchimba(tab.id)}
            className={`flex h-11 shrink-0 items-center gap-2 whitespace-nowrap border-b-2 text-sm transition duration-200 ${
              selectat
                ? 'border-primary font-semibold text-primary'
                : 'border-transparent text-ink2 hover:text-ink'
            }`}
          >
            {tab.icon}
            {tab.eticheta}
          </button>
        );
      })}
    </div>
  );
}
