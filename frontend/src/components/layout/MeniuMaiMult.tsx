import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ThemeToggle } from '../ui/ThemeToggle';
import { ButonIcon } from '../ui/ButonIcon';
import { IconUser, IconX } from '../ui/icons';
import { CardPro } from './CardPro';
import { IN_BARA_JOS, SECTIUNI } from './navigatie';

/** Foaia de jos „Mai mult" (mobil): sectiunile care nu incap in bara, profilul si PRO. */
export function MeniuMaiMult({ onInchide, activa }: { onInchide: () => void; activa?: string }) {
  const restul = SECTIUNI.filter((s) => !IN_BARA_JOS.includes(s.eticheta));

  useEffect(() => {
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && onInchide();
    document.addEventListener('keydown', esc);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', esc);
      document.body.style.overflow = overflow;
    };
  }, [onInchide]);

  return (
    <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Mai mult">
      <button
        type="button"
        aria-label="Închide meniul"
        onClick={onInchide}
        className="absolute inset-0 animate-fade-in bg-[#020510]/70 backdrop-blur-sm"
      />
      <div className="pb-safe absolute inset-x-0 bottom-0 max-h-[85vh] animate-urcare overflow-y-auto rounded-t-[26px] border-t border-line bg-card px-4 pt-2 shadow-[0_-12px_40px_rgba(0,0,0,0.35)]">
        <div className="mx-auto mb-3 h-1.5 w-10 rounded-full bg-ink2/30" />
        <div className="mb-3 flex items-center justify-between">
          <p className="text-lg font-extrabold text-ink">Mai mult</p>
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <ButonIcon eticheta="Închide" onClick={onInchide}>
              <IconX width={20} height={20} />
            </ButonIcon>
          </div>
        </div>

        <ul className="grid grid-cols-3 gap-2.5 sm:grid-cols-4">
          {restul.map((s) => {
            const Icon = s.icon;
            const eActiva = activa === s.eticheta;
            return (
              <li key={s.eticheta}>
                <Link
                  to={s.to}
                  onClick={onInchide}
                  aria-current={eActiva ? 'page' : undefined}
                  className={`flex h-[84px] flex-col items-center justify-center gap-2 rounded-2xl border text-xs font-semibold transition active:scale-95 ${
                    eActiva
                      ? 'border-primary/50 bg-primary/15 text-primary'
                      : 'border-line bg-bg/60 text-ink hover:border-primary/40'
                  }`}
                >
                  <Icon width={24} height={24} className={eActiva ? 'text-primary' : 'text-ink2'} />
                  {s.eticheta}
                </Link>
              </li>
            );
          })}
          <li>
            <Link
              to="/profil"
              onClick={onInchide}
              className="flex h-[84px] flex-col items-center justify-center gap-2 rounded-2xl border border-line bg-bg/60 text-xs font-semibold text-ink transition hover:border-primary/40 active:scale-95"
            >
              <IconUser width={24} height={24} className="text-ink2" />
              Profil
            </Link>
          </li>
        </ul>

        <div className="my-4">
          <CardPro compact />
        </div>
      </div>
    </div>
  );
}
