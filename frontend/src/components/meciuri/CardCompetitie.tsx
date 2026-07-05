import { useState } from 'react';
import type { LigaZi } from '../../api/types';
import { numeLiga } from '../../lib/ligi';
import { Card } from '../ui/Card';
import { LigaLogo } from '../ui/LigaLogo';
import { IconChevronDown } from '../ui/icons';
import { RandMeciPremium } from './RandMeciPremium';

/** Cate meciuri aratam inainte de "Vezi toate cele N meciuri". */
const PRAG_MECIURI = 6;

/** Runda competitiei = runda primului meci al zilei (text liber din sursa), scurtata pentru afisare. */
function etichetaRunda(liga: LigaZi): string | null {
  const runda = liga.meciuri.find((m) => m.runda)?.runda;
  if (!runda) return null;
  // "Regular Season - 12" → "Etapa 12"; altfel pastram textul brut, curatat.
  const m = runda.match(/(\d+)\s*$/);
  if (/regular season|matchday|round|etapa/i.test(runda) && m) return `Etapa ${m[1]}`;
  return runda;
}

/**
 * O competitie = un card mare (design system: alb, radius 16, umbra subtila). Antet cu logo, nume,
 * tara + runda si numarul de meciuri; expandat implicit, cu colaps si "Vezi toate cele N meciuri".
 */
export function CardCompetitie({ liga }: { liga: LigaZi }) {
  const [deschis, setDeschis] = useState(true);
  const [toate, setToate] = useState(false);

  const nume = liga.nume ?? numeLiga(liga.leagueId);
  const runda = etichetaRunda(liga);
  const subtitlu = [liga.tara, runda].filter(Boolean).join(' · ');
  const vizibile = toate ? liga.meciuri : liga.meciuri.slice(0, PRAG_MECIURI);
  const areLive = liga.meciuri.some((m) => m.inDesfasurare);

  return (
    <Card className="overflow-hidden">
      <button
        type="button"
        onClick={() => setDeschis((d) => !d)}
        className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition hover:bg-bg"
      >
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 dark:bg-primary/20">
          <LigaLogo id={liga.leagueId} nume={nume} size={22} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-sm font-extrabold text-ink">{nume}</p>
            {areLive && (
              <span className="flex items-center gap-1 rounded-full bg-accent/10 px-1.5 py-0.5 text-[10px] font-bold uppercase text-accent">
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />
                Live
              </span>
            )}
          </div>
          {subtitlu && <p className="truncate text-xs text-ink2">{subtitlu}</p>}
        </div>
        <span className="shrink-0 text-xs font-semibold text-ink2">
          {liga.meciuri.length} {liga.meciuri.length === 1 ? 'meci' : 'meciuri'}
        </span>
        <IconChevronDown
          width={18}
          height={18}
          className={`shrink-0 text-ink2 transition-transform ${deschis ? '' : '-rotate-90'}`}
        />
      </button>

      {deschis && (
        <div className="border-t border-line p-2">
          <div className="space-y-0.5">
            {vizibile.map((meci) => (
              <RandMeciPremium key={meci.fixtureId} meci={meci} />
            ))}
          </div>

          {liga.meciuri.length > PRAG_MECIURI && (
            <button
              type="button"
              onClick={() => setToate((t) => !t)}
              className="mt-1 w-full rounded-2xl py-2.5 text-center text-xs font-bold uppercase tracking-wide text-primary transition hover:bg-primary/5"
            >
              {toate ? 'Arată mai puține' : `Vezi toate cele ${liga.meciuri.length} meciuri`}
            </button>
          )}
        </div>
      )}
    </Card>
  );
}
