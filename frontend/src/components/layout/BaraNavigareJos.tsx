import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { IconGrid } from '../ui/icons';
import { MeniuMaiMult } from './MeniuMaiMult';
import { IN_BARA_JOS, SECTIUNI, sectiuneActiva } from './navigatie';

/** Bara de navigare fixa de jos (mobil + tableta): patru sectiuni principale + „Mai mult". */
export function BaraNavigareJos() {
  const { pathname } = useLocation();
  const [meniu, setMeniu] = useState(false);
  const activa = sectiuneActiva(pathname);
  const principale = SECTIUNI.filter((s) => IN_BARA_JOS.includes(s.eticheta));
  const inMeniu = activa != null && !IN_BARA_JOS.includes(activa.eticheta);

  useEffect(() => setMeniu(false), [pathname]);

  return (
    <>
      <nav
        aria-label="Navigație principală"
        className="pb-safe fixed inset-x-0 bottom-0 z-30 border-t border-line/70 bg-card/90 backdrop-blur-xl lg:hidden"
      >
        <ul className="mx-auto grid h-16 max-w-2xl grid-cols-5">
          {principale.map((s) => {
            const Icon = s.icon;
            const eActiva = activa?.eticheta === s.eticheta;
            return (
              <li key={s.eticheta}>
                <Link
                  to={s.to}
                  aria-current={eActiva ? 'page' : undefined}
                  className={`relative flex h-full flex-col items-center justify-center gap-1 text-[11px] font-semibold transition active:scale-95 ${
                    eActiva ? 'text-primary' : 'text-ink2'
                  }`}
                >
                  {eActiva && <span className="absolute top-0 h-[3px] w-8 rounded-b-full bg-primary" />}
                  <span className="relative">
                    <Icon width={23} height={23} strokeWidth={eActiva ? 2.1 : 1.8} />
                    {s.eticheta === 'Live' && (
                      <span className="absolute -right-1 -top-0.5 h-2 w-2 animate-pulse rounded-full bg-accent" />
                    )}
                  </span>
                  {s.eticheta}
                </Link>
              </li>
            );
          })}
          <li>
            <button
              type="button"
              onClick={() => setMeniu(true)}
              aria-expanded={meniu}
              className={`relative flex h-full w-full flex-col items-center justify-center gap-1 text-[11px] font-semibold transition active:scale-95 ${
                inMeniu || meniu ? 'text-primary' : 'text-ink2'
              }`}
            >
              {inMeniu && <span className="absolute top-0 h-[3px] w-8 rounded-b-full bg-primary" />}
              <IconGrid width={23} height={23} />
              Mai mult
            </button>
          </li>
        </ul>
      </nav>

      {meniu && <MeniuMaiMult onInchide={() => setMeniu(false)} activa={activa?.eticheta} />}
    </>
  );
}
