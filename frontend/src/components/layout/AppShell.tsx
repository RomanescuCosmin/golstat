import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav';
import { Sidebar } from './Sidebar';

/**
 * Scheletul aplicatiei: TopNav sus, Sidebar la stanga, continutul rutei in centru.
 * Paginile isi pot adauga propriul sidebar drept in interiorul zonei de continut.
 */
export function AppShell() {
  return (
    // fundalul plat vine de pe <body> (bg-bg); aici punem doar decorul fix, sub tot continutul
    <div className="min-h-screen text-ink">
      {/* Decor global: stadionul nocturn, fix sub continut; se topeste in culoarea fundalului spre baza. */}
      <div aria-hidden className="pointer-events-none fixed inset-0 -z-10">
        <img
          src="/stadion-fundal.jpg"
          alt=""
          className="h-full w-full object-cover object-top opacity-[0.65] dark:opacity-[0.55]"
        />
        {/* Valul de estompare sta jos (70% pe light): imaginea ramane curata, fara umbre albe pe mijloc. */}
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-bg/10 via-70% to-bg dark:from-bg/10 dark:via-bg/45 dark:via-50%" />
      </div>

      <TopNav />
      <div className="flex">
        <Sidebar />
        <main className="min-w-0 flex-1 px-4 py-5 lg:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
