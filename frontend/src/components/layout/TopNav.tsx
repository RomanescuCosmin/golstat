import { NavLink } from 'react-router-dom';
import { MingeGolStat } from '../ui/MingeGolStat';
import { ThemeToggle } from '../ui/ThemeToggle';
import { IconClopot } from '../ui/icons';
import { Cautare } from './Cautare';

function Logo() {
  // Marca GolStat (aceeasi peste tot: navbar, favicon, spinner, splash) + wordmark.
  return (
    <NavLink to="/" className="group flex items-center gap-2" aria-label="golstat — acasă">
      <MingeGolStat size={36} className="animate-floaty transition-transform duration-200 group-hover:rotate-[16deg] group-hover:scale-110 group-hover:animate-none motion-reduce:animate-none" />
      <span className="text-2xl font-extrabold tracking-tight">
        <span className="text-primary">gol</span>
        <span className="text-accent">stat</span>
      </span>
    </NavLink>
  );
}

const tabClasa = ({ isActive }: { isActive: boolean }) =>
  `relative flex h-[72px] items-center gap-1.5 border-b-2 px-1 text-sm font-semibold transition ${
    isActive
      ? 'border-primary text-primary'
      : 'border-transparent text-ink2 hover:text-ink'
  }`;

export function TopNav() {
  return (
    <header className="sticky top-0 z-20 border-b border-line bg-card shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
      <div className="flex h-[72px] items-center gap-8 px-4 lg:px-6">
        <Logo />

        <nav className="hidden h-[72px] items-center gap-6 md:flex lg:gap-10" aria-label="Navigație principală">
          <NavLink to="/" end className={tabClasa}>
            Meciuri
          </NavLink>
          <NavLink to="/live" className={tabClasa}>
            Live
            <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-bold text-white">
              •
            </span>
          </NavLink>
          <NavLink to="/program" className={tabClasa}>
            Program
          </NavLink>
          <NavLink to="/competitie/39" className={tabClasa}>
            Competiții
          </NavLink>
          <NavLink to="/statistici" className={tabClasa}>
            Statistici
          </NavLink>
          <NavLink to="/piete" className={tabClasa}>
            Piețe
          </NavLink>
          <NavLink to="/echipe" className={tabClasa}>
            Echipe
          </NavLink>
          {['Știri'].map((eticheta) => (
            <span
              key={eticheta}
              title="În curând"
              aria-disabled
              className="flex h-[72px] cursor-not-allowed items-center border-b-2 border-transparent px-1 text-sm font-semibold text-ink2/60"
            >
              {eticheta}
            </span>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2 lg:gap-3">
          <Cautare />

          <ThemeToggle />

          <button
            type="button"
            title="Notificări"
            aria-label="Notificări"
            className="relative flex h-9 w-9 items-center justify-center rounded-full text-ink2 transition hover:bg-bg hover:text-ink"
          >
            <IconClopot width={19} height={19} />
            <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full border-2 border-card bg-accent" />
          </button>

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
