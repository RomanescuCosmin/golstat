import { NavLink } from 'react-router-dom';
import type { ComponentType, SVGProps } from 'react';
import {
  IconCalendar,
  IconChart,
  IconCheck,
  IconChevronRight,
  IconDiamond,
  IconGear,
  IconLive,
  IconShield,
  IconStar,
  IconStiri,
  IconTrophy,
  IconUser,
} from '../ui/icons';

type Icon = ComponentType<SVGProps<SVGSVGElement>>;

interface Item {
  eticheta: string;
  icon: Icon;
  to?: string;
  end?: boolean;
}

const iteme: Item[] = [
  { eticheta: 'Meciuri', icon: IconCalendar, to: '/', end: true },
  { eticheta: 'Live', icon: IconLive, to: '/live' },
  { eticheta: 'Program', icon: IconCalendar, to: '/program' },
  { eticheta: 'Competiții', icon: IconTrophy, to: '/competitie/39' },
  { eticheta: 'Echipe', icon: IconShield, to: '/echipe' },
  { eticheta: 'Jucători', icon: IconUser, to: '/jucatori' },
  { eticheta: 'Statistici', icon: IconChart, to: '/statistici' },
  { eticheta: 'Știri', icon: IconStiri },
  { eticheta: 'Favorite', icon: IconStar },
  { eticheta: 'Setări', icon: IconGear },
];

function ElementNav({ item }: { item: Item }) {
  const Icon = item.icon;

  if (!item.to) {
    return (
      <span
        title="În curând"
        aria-disabled
        className="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-ink2/60"
      >
        <Icon width={18} height={18} />
        {item.eticheta}
      </span>
    );
  }

  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        `relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
          isActive
            ? 'bg-primary/10 font-semibold text-primary dark:bg-primary/15'
            : 'text-ink2 hover:bg-bg hover:text-ink'
        }`
      }
    >
      {({ isActive }) => (
        <>
          {isActive && <span className="absolute -left-3 top-1/2 h-6 w-1 -translate-y-1/2 rounded-r bg-primary" />}
          <Icon width={18} height={18} />
          {item.eticheta}
        </>
      )}
    </NavLink>
  );
}

function CardPro() {
  const beneficii = ['Statistici avansate', 'Fără reclame', 'Alerte meciuri', 'Compară jucători'];
  return (
    <div className="mx-3 mb-4 rounded-card border border-line bg-primary/5 p-4 dark:bg-primary/10">
      <p className="flex items-center gap-2 text-sm font-bold">
        <IconDiamond width={18} height={18} className="text-primary" />
        <span className="text-primary">golstat</span>
        <span className="text-accent">PRO</span>
      </p>
      <ul className="mt-3 space-y-2">
        {beneficii.map((b) => (
          <li key={b} className="flex items-center gap-2 text-xs text-ink2">
            <IconCheck width={14} height={14} className="shrink-0 text-primary" />
            {b}
          </li>
        ))}
      </ul>
      <button
        type="button"
        title="În curând"
        className="mt-4 flex w-full items-center justify-between rounded-lg bg-gradient-to-r from-primary to-accent px-4 py-2.5 text-sm font-semibold text-white transition hover:opacity-90"
      >
        Upgrade acum
        <IconChevronRight width={16} height={16} />
      </button>
    </div>
  );
}

export function Sidebar() {
  return (
    <aside className="sticky top-[72px] hidden h-[calc(100vh-72px)] w-60 shrink-0 flex-col overflow-y-auto border-r border-line bg-card lg:flex">
      <nav className="flex-1 space-y-1 p-3" aria-label="Navigație laterală">
        {iteme.map((item) => (
          <ElementNav key={item.eticheta} item={item} />
        ))}
      </nav>
      <CardPro />
    </aside>
  );
}
