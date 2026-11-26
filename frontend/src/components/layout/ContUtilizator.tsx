import { Link } from 'react-router-dom';

/**
 * Avatarul contului din header. Punctul unic de legatura cu autentificarea (in lucru separat):
 * cand exista sesiune, numele/initiala vin de aici, fara sa atinga restul shell-ului.
 */
export function ContUtilizator({ cuNume = false }: { cuNume?: boolean }) {
  return (
    <Link
      to="/profil"
      aria-label="Profil"
      title="Profil"
      className="flex items-center gap-2 rounded-full transition hover:opacity-90"
    >
      <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-[#7C3AED] text-sm font-bold text-white ring-2 ring-primary/20">
        A
      </span>
      {cuNume && (
        <span className="hidden leading-tight xl:block">
          <span className="block text-sm font-semibold text-ink">Alex Pop</span>
          <span className="block text-[11px] font-bold text-accent">PRO</span>
        </span>
      )}
    </Link>
  );
}
