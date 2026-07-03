import { NavLink } from 'react-router-dom';
import { ThemeToggle } from '../ui/ThemeToggle';
import { IconSearch } from '../ui/icons';

function Logo() {
  // Cerc din trei arce colorate (albastru / rosu / navy), ca in design.
  return (
    <NavLink to="/" className="flex items-center gap-2" aria-label="golstat — acasă">
      <svg width="34" height="34" viewBox="0 0 36 36" aria-hidden>
        <circle cx="18" cy="18" r="13" fill="none" stroke="#2151E5" strokeWidth="6" strokeLinecap="round" strokeDasharray="24 57.7" strokeDashoffset="0" />
        <circle cx="18" cy="18" r="13" fill="none" stroke="#E23B3B" strokeWidth="6" strokeLinecap="round" strokeDasharray="24 57.7" strokeDashoffset="-27.2" />
        <circle cx="18" cy="18" r="13" fill="none" stroke="#1A2233" strokeWidth="6" strokeLinecap="round" strokeDasharray="24 57.7" strokeDashoffset="-54.4" className="dark:stroke-[#E6EAF2]" />
      </svg>
      <span className="text-2xl font-extrabold tracking-tight">
        <span className="text-ink">gol</span>
        <span className="text-accent">stat</span>
      </span>
    </NavLink>
  );
}

const tabClasa = ({ isActive }: { isActive: boolean }) =>
  `relative flex h-16 items-center gap-1.5 border-b-2 px-1 text-sm font-semibold transition ${
    isActive
      ? 'border-primary text-primary'
      : 'border-transparent text-ink2 hover:text-ink'
  }`;

export function TopNav() {
  return (
    <header className="sticky top-0 z-20 border-b border-line bg-card">
      <div className="flex h-16 items-center gap-8 px-4 lg:px-6">
        <Logo />

        <nav className="hidden h-16 items-center gap-6 md:flex" aria-label="Navigație principală">
          <NavLink to="/" end className={tabClasa}>
            Meciuri
          </NavLink>
          <NavLink to="/live" className={tabClasa}>
            Live
            <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-bold text-white">
              •
            </span>
          </NavLink>
          {['Competiții', 'Statistici', 'Echipe'].map((eticheta) => (
            <span
              key={eticheta}
              title="În curând"
              aria-disabled
              className="flex h-16 cursor-not-allowed items-center border-b-2 border-transparent px-1 text-sm font-semibold text-ink2/60"
            >
              {eticheta}
            </span>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2 lg:gap-3">
          <div className="relative hidden lg:block">
            <IconSearch className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink2" width={16} height={16} />
            <input
              type="search"
              placeholder="Caută echipe, competiții…"
              disabled
              title="În curând"
              className="h-9 w-56 rounded-full border border-line bg-bg pl-9 pr-3 text-sm text-ink placeholder:text-ink2 focus:outline-none focus:ring-2 focus:ring-primary/40 disabled:cursor-not-allowed"
            />
          </div>

          <ThemeToggle />

          <div className="flex items-center gap-2 pl-1">
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary dark:bg-primary/20">
              A
            </span>
            <span className="hidden leading-tight sm:block">
              <span className="block text-sm font-semibold text-ink">Alex Pop</span>
              <span className="block text-[11px] font-bold text-accent">PRO</span>
            </span>
          </div>
        </div>
      </div>
    </header>
  );
}
