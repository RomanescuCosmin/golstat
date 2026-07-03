import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav';
import { Sidebar } from './Sidebar';

/**
 * Scheletul aplicatiei: TopNav sus, Sidebar la stanga, continutul rutei in centru.
 * Paginile isi pot adauga propriul sidebar drept in interiorul zonei de continut.
 */
export function AppShell() {
  return (
    <div className="min-h-screen bg-bg text-ink">
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
