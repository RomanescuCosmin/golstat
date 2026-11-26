import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { ContainerNotificariGol } from '../live/NotificareGol';
import { BaraNavigareJos } from './BaraNavigareJos';
import { HeaderAplicatie } from './HeaderAplicatie';
import { Sidebar } from './Sidebar';

/**
 * Scheletul aplicatiei. Mobil/tableta: header compact sus + bara de navigare fixa jos, continut pe
 * o coloana. Desktop (lg+): header lat + sidebar stang, continutul rutei in centru.
 */
export function AppShell() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo?.({ top: 0 });
  }, [pathname]);

  return (
    // fundalul plat vine de pe <body> (bg-bg); aici punem doar decorul fix, sub tot continutul
    <div className="min-h-screen text-ink">
      <div aria-hidden className="pointer-events-none fixed inset-0 -z-10">
        <img
          src="/stadion-fundal.jpg"
          alt=""
          className="h-full w-full object-cover object-top opacity-[0.14] dark:opacity-[0.32]"
        />
        {/* Pe light, valul gros opreste halourile reflectoarelor in jurul cardurilor; pe dark, stadionul
            se topeste spre navy-ul fundalului. */}
        <div className="absolute inset-0 bg-gradient-to-b from-bg/45 via-bg/80 via-55% to-bg dark:from-bg/30 dark:via-bg/75 dark:via-40%" />
      </div>

      <HeaderAplicatie />
      <div className="flex">
        <Sidebar />
        <main className="pb-bara-jos min-w-0 flex-1 px-3 pt-4 sm:px-5 lg:px-6 lg:pb-8 lg:pt-6">
          <Outlet />
        </main>
      </div>
      <BaraNavigareJos />

      {/* Notificari de gol pentru meciurile echipelor favorite (global, peste tot continutul). */}
      <ContainerNotificariGol />
    </div>
  );
}
