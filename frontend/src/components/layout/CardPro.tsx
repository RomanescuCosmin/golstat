import { Link } from 'react-router-dom';
import { IconCheck, IconChevronRight, IconDiamond } from '../ui/icons';

export const BENEFICII_PRO = ['Statistici avansate', 'Fără reclame', 'Alerte meciuri', 'Compară jucători'];

/** Cardul golstat PRO (sidebar, meniul mobil, profil); duce la pagina de profil. */
export function CardPro({ compact = false }: { compact?: boolean }) {
  return (
    <div className="relative overflow-hidden rounded-card border border-primary/25 bg-gradient-to-br from-primary/15 via-card to-[#7C3AED]/10 p-4">
      <span aria-hidden className="pointer-events-none absolute -right-8 -top-8 h-24 w-24 rounded-full bg-primary/20 blur-2xl" />
      <p className="flex items-center gap-2 text-sm font-bold">
        <IconDiamond width={18} height={18} className="text-primary" />
        <span className="text-primary">golstat</span>
        <span className="rounded-md bg-accent px-1.5 py-0.5 text-[10px] font-extrabold text-white">PRO</span>
      </p>
      {!compact && (
        <ul className="mt-3 space-y-2">
          {BENEFICII_PRO.map((b) => (
            <li key={b} className="flex items-center gap-2 text-xs text-ink2">
              <IconCheck width={14} height={14} className="shrink-0 text-primary" />
              {b}
            </li>
          ))}
        </ul>
      )}
      <Link
        to="/profil"
        className="mt-4 flex w-full items-center justify-between rounded-btn bg-gradient-to-r from-primary to-[#7C3AED] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_6px_20px_rgb(var(--gs-primary)/0.35)] transition hover:opacity-90"
      >
        Upgrade acum
        <IconChevronRight width={16} height={16} />
      </Link>
    </div>
  );
}
