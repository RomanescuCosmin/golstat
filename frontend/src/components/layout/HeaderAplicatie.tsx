import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ThemeToggle } from '../ui/ThemeToggle';
import { ButonIcon } from '../ui/ButonIcon';
import { IconArrowLeft, IconSearch, IconX } from '../ui/icons';
import { MingeGolStat } from '../ui/MingeGolStat';
import { Cautare } from './Cautare';
import { ContUtilizator } from './ContUtilizator';
import { Logo } from './Logo';
import { antetRuta } from './navigatie';

/**
 * Header-ul aplicatiei. Mobil/tableta: bara compacta (marca sau „inapoi", titlul sectiunii, cautare, profil).
 * Desktop: logo pe latimea sidebar-ului, cautarea inline, tema si profil.
 */
export function HeaderAplicatie() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { titlu, detaliu } = antetRuta(pathname);
  const [cautareMobil, setCautareMobil] = useState(false);

  useEffect(() => setCautareMobil(false), [pathname]);

  return (
    <header className="sticky top-0 z-30 border-b border-line/70 bg-card/85 backdrop-blur-xl supports-[backdrop-filter]:bg-card/70">
      {/* mobil + tableta */}
      <div className="flex h-14 items-center gap-1 px-2 sm:px-4 lg:hidden">
        {detaliu ? (
          <ButonIcon eticheta="Înapoi" onClick={() => navigate(-1)} className="text-ink">
            <IconArrowLeft width={22} height={22} />
          </ButonIcon>
        ) : null}
        {/* mingea + wordmark-ul duc acasa; pe paginile de detaliu mingea face loc butonului „inapoi" */}
        <Link to="/" aria-label="golstat — acasă" className="group flex min-w-0 flex-1 items-center gap-1">
          {!detaliu && (
            <span className="flex h-10 w-10 shrink-0 items-center justify-center">
              <MingeGolStat
                size={30}
                className="transition-transform duration-200 group-hover:rotate-[16deg] motion-reduce:transition-none"
              />
            </span>
          )}
          <span className="min-w-0 px-1">
            <span className="block truncate text-[17px] font-extrabold leading-tight text-ink">{titlu}</span>
            <span className="block text-[10px] font-bold uppercase leading-tight tracking-[0.14em]">
              <span className="text-primary">gol</span>
              <span className="text-accent">stat</span>
            </span>
          </span>
        </Link>
        <ButonIcon eticheta="Caută" onClick={() => setCautareMobil(true)}>
          <IconSearch width={20} height={20} />
        </ButonIcon>
        <span className="ml-1">
          <ContUtilizator />
        </span>
      </div>

      {cautareMobil && (
        <div className="absolute inset-x-0 top-0 z-40 flex h-14 items-center gap-2 bg-card px-3 lg:hidden">
          <div className="min-w-0 flex-1">
            <Cautare plin onAles={() => setCautareMobil(false)} />
          </div>
          <ButonIcon eticheta="Închide căutarea" onClick={() => setCautareMobil(false)}>
            <IconX width={20} height={20} />
          </ButonIcon>
        </div>
      )}

      {/* desktop */}
      <div className="hidden h-16 items-center gap-4 pr-6 lg:flex">
        <div className="flex w-64 shrink-0 items-center px-5">
          <Logo />
        </div>
        <Cautare />
        <div className="ml-auto flex items-center gap-2">
          <ThemeToggle />
          <span className="pl-1">
            <ContUtilizator cuNume />
          </span>
        </div>
      </div>
    </header>
  );
}
