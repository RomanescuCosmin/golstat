import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { EchipaDto } from '../../api/types';
import { useNotificariGol, type NotificareGol } from '../../hooks/useNotificariGol';
import { pregatesteSunet } from '../../lib/sunet';
import { numeEchipa } from '../../lib/echipa';
import { TeamLogo } from '../ui/TeamLogo';

/** Un rand de echipa in card: logo + nume + scor; eticheta „GOOL" pe randul echipei care a inscris. */
function RandEchipa({ echipa, gol, aInscris }: { echipa: EchipaDto; gol: number; aInscris: boolean }) {
  return (
    <div className={`flex items-center gap-2 ${aInscris ? 'text-ink' : 'text-ink2'}`}>
      <TeamLogo nume={echipa.nume} logo={echipa.logo} size={22} />
      <span className={`min-w-0 flex-1 truncate text-sm ${aInscris ? 'font-extrabold' : 'font-medium'}`}>
        {numeEchipa(echipa)}
      </span>
      {aInscris && (
        <span className="rounded-full bg-win px-1.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-white">
          ⚽ Gool
        </span>
      )}
      <span className="w-5 text-right text-sm font-bold tabular-nums">{gol}</span>
    </div>
  );
}

/** Cardul unei notificari de gol (prezentational; exportat pentru teste). */
export function CardNotificare({ n, onClose }: { n: NotificareGol; onClose: () => void }) {
  const [vizibil, setVizibil] = useState(false);
  useEffect(() => {
    const id = requestAnimationFrame(() => setVizibil(true));
    return () => cancelAnimationFrame(id);
  }, []);

  return (
    <div
      role="status"
      className={`pointer-events-auto w-full overflow-hidden rounded-card border border-line bg-card shadow-card transition-all duration-300 ${
        vizibil ? 'translate-x-0 opacity-100' : 'translate-x-6 opacity-0'
      }`}
    >
      <div className="flex items-center justify-between border-b border-line bg-win/10 px-3 py-1.5">
        <span className="flex items-center gap-1.5 text-xs font-bold text-win">
          <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-win" />
          GOOL{n.minut != null ? ` · ${n.minut}'` : ''}
        </span>
        <button
          type="button"
          onClick={onClose}
          aria-label="Închide"
          className="text-lg leading-none text-ink2 transition hover:text-ink"
        >
          ×
        </button>
      </div>
      <Link
        to={`/meci/${n.fixtureId}/centru`}
        onClick={onClose}
        className="block space-y-1.5 px-3 py-2 transition hover:bg-bg"
      >
        {n.ligaNume && <p className="truncate text-[11px] text-ink2">{n.ligaNume}</p>}
        <RandEchipa echipa={n.gazde} gol={n.golGazde} aInscris={n.marcatorAcasa} />
        <RandEchipa echipa={n.oaspeti} gol={n.golOaspeti} aInscris={!n.marcatorAcasa} />
      </Link>
    </div>
  );
}

/**
 * Containerul global al notificarilor de gol (montat in AppShell → apare pe orice pagina).
 * Deblocheaza si audio la primul gest al utilizatorului (politica de autoplay a browserelor).
 */
export function ContainerNotificariGol() {
  const { notificari, inchide } = useNotificariGol();

  useEffect(() => {
    const deblocheaza = () => pregatesteSunet();
    window.addEventListener('pointerdown', deblocheaza, { once: true });
    window.addEventListener('keydown', deblocheaza, { once: true });
    return () => {
      window.removeEventListener('pointerdown', deblocheaza);
      window.removeEventListener('keydown', deblocheaza);
    };
  }, []);

  if (notificari.length === 0) {
    return null;
  }

  return (
    <div className="pointer-events-none fixed right-4 top-20 z-50 flex w-[calc(100vw-2rem)] max-w-xs flex-col gap-2 sm:w-80">
      {notificari.map((n) => (
        <CardNotificare key={n.cheie} n={n} onClose={() => inchide(n.cheie)} />
      ))}
    </div>
  );
}
