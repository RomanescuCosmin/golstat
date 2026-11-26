import { Link, useLocation } from 'react-router-dom';
import { CardPro } from './CardPro';
import { SECTIUNI, sectiuneActiva, type SectiuneNav } from './navigatie';

function ElementNav({ item, activ }: { item: SectiuneNav; activ: boolean }) {
  const Icon = item.icon;
  return (
    <Link
      to={item.to}
      aria-current={activ ? 'page' : undefined}
      className={`relative flex items-center gap-3 rounded-btn px-3 py-2.5 text-sm font-medium transition ${
        activ
          ? 'bg-primary/15 font-semibold text-primary'
          : 'text-ink2 hover:bg-ink2/10 hover:text-ink'
      }`}
    >
      {activ && <span className="absolute -left-3 top-1/2 h-6 w-1 -translate-y-1/2 rounded-r bg-primary" />}
      <Icon width={19} height={19} />
      {item.eticheta}
    </Link>
  );
}

/** Navigatia laterala de pe desktop (lg+); pe mobil/tableta navigarea e bara de jos. */
export function Sidebar() {
  const { pathname } = useLocation();
  const activa = sectiuneActiva(pathname);

  return (
    <aside className="sticky top-16 hidden h-[calc(100vh-64px)] w-64 shrink-0 flex-col overflow-y-auto border-r border-line/70 bg-card/60 backdrop-blur lg:flex">
      <nav className="flex-1 space-y-1 p-3" aria-label="Navigație laterală">
        {SECTIUNI.map((item) => (
          <ElementNav key={item.eticheta} item={item} activ={activa?.eticheta === item.eticheta} />
        ))}
      </nav>
      <div className="p-3">
        <CardPro />
      </div>
    </aside>
  );
}
