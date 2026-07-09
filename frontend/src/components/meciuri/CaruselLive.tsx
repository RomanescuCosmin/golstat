import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { useNavigate } from 'react-router-dom';
import { getLive } from '../../api/client';
import type { EchipaDto, MeciLive } from '../../api/types';
import { useScoreFlash } from '../../hooks/useAnimatii';
import { prefersReducedMotion } from '../../hooks/useCountUp';
import { numeEchipa } from '../../lib/echipa';
import { numeLiga } from '../../lib/ligi';
import { Card } from '../ui/Card';
import { TeamLogo } from '../ui/TeamLogo';
import { IconChevronLeft, IconChevronRight } from '../ui/icons';

const REIMPROSPATARE_MS = 15000;
const LATIME_CARD = 200;
const PAS = LATIME_CARD + 16; // card + spatiul dintre carduri
const VITEZA_PX_S = 26;
const NUDGE_MS = 480;

/** Tente de culoare pentru carduri, ciclic — ca in mockup fiecare meci are alta nuanta subtila. */
const TINTE = ['139 92 246', '244 63 94', '59 130 246', '16 185 129', '245 158 11', '99 102 241'];

/**
 * Sectiunea "Meciuri în desfășurare": carusel compact, cu derulare continua uniforma
 * (marquee fara capete), pauza lina la hover si salt animat din sageti. Carduri mici cu tenta
 * de culoare: minut live, cate un rand pe echipa cu scorul si bara de desfasurare.
 */
export function CaruselLive() {
  const navigate = useNavigate();
  const [live, setLive] = useState<MeciLive[]>([]);

  useEffect(() => {
    let anulat = false;
    const incarca = () => {
      getLive()
        .then((r) => {
          if (!anulat) setLive(r);
        })
        .catch(() => {
          if (!anulat) setLive([]);
        });
    };
    incarca();
    const t = setInterval(incarca, REIMPROSPATARE_MS);
    return () => {
      anulat = true;
      clearInterval(t);
    };
  }, []);

  return (
    <Card className="overflow-hidden">
      <div className="flex items-center gap-2.5 px-5 pb-1 pt-4">
        <h2 className="text-[15px] font-extrabold text-ink">Meciuri în desfășurare</h2>
        {live.length > 0 && (
          <span className="flex items-center gap-1.5 rounded-full bg-accent/10 px-2 py-0.5 text-[11px] font-bold text-accent">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />
            {live.length}
          </span>
        )}
      </div>

      {live.length === 0 ? (
        <p className="px-5 pb-5 pt-2 text-xs text-ink2">Niciun meci în desfășurare acum.</p>
      ) : (
        <Banda live={live} onOpen={(id) => navigate(`/meci/${id}/centru`)} />
      )}
    </Card>
  );
}

/** Banda derulanta propriu-zisa: motor rAF cu viteza constanta si continut duplicat pentru bucla. */
function Banda({ live, onOpen }: { live: MeciLive[]; onOpen: (fixtureId: number) => void }) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);
  const offsetRef = useRef(0);
  const vitezaRef = useRef(0);
  const tintaVitezaRef = useRef(VITEZA_PX_S);
  const nudgeRef = useRef<{ de: number; delta: number; t0: number } | null>(null);
  const [latimeViewport, setLatimeViewport] = useState(0);
  const faraMiscare = prefersReducedMotion();

  const perioada = live.length * PAS;
  const depaseste = latimeViewport > 0 && perioada > latimeViewport;
  const ruleaza = !faraMiscare && depaseste;
  const copii = ruleaza ? Math.max(2, Math.ceil(latimeViewport / perioada) + 1) : 1;

  useEffect(() => {
    const el = viewportRef.current;
    if (!el) return;
    const ro = new ResizeObserver(() => setLatimeViewport(el.clientWidth));
    ro.observe(el);
    setLatimeViewport(el.clientWidth);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    if (!ruleaza) {
      offsetRef.current = 0;
      if (trackRef.current) trackRef.current.style.transform = 'translate3d(0,0,0)';
      return;
    }
    let raf = 0;
    let prev = performance.now();
    const pas = (t: number) => {
      // dt plafonat: la revenirea in tab nu sarim brusc inainte
      const dt = Math.min(t - prev, 64) / 1000;
      prev = t;
      const n = nudgeRef.current;
      if (n) {
        const p = Math.min(1, (t - n.t0) / NUDGE_MS);
        offsetRef.current = n.de + n.delta * (1 - Math.pow(1 - p, 3));
        if (p >= 1) nudgeRef.current = null;
      } else {
        vitezaRef.current += (tintaVitezaRef.current - vitezaRef.current) * Math.min(1, dt * 6);
        offsetRef.current += vitezaRef.current * dt;
      }
      const off = ((offsetRef.current % perioada) + perioada) % perioada;
      if (trackRef.current) trackRef.current.style.transform = `translate3d(${-off}px,0,0)`;
      raf = requestAnimationFrame(pas);
    };
    raf = requestAnimationFrame(pas);
    return () => cancelAnimationFrame(raf);
  }, [ruleaza, perioada]);

  const sare = (directie: number) => {
    if (!ruleaza) {
      viewportRef.current?.scrollBy({ left: directie * PAS, behavior: 'smooth' });
      return;
    }
    nudgeRef.current = { de: offsetRef.current, delta: directie * PAS, t0: performance.now() };
  };

  return (
    <div className="relative px-4 pb-4 pt-2">
      <div
        ref={viewportRef}
        onPointerEnter={() => {
          tintaVitezaRef.current = 0;
        }}
        onPointerLeave={() => {
          tintaVitezaRef.current = VITEZA_PX_S;
        }}
        className={
          ruleaza
            ? 'overflow-hidden py-1 [mask-image:linear-gradient(90deg,transparent,black_44px,black_calc(100%-44px),transparent)]'
            : 'overflow-x-auto py-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden'
        }
      >
        <div ref={trackRef} className="flex w-max will-change-transform">
          {Array.from({ length: copii }).flatMap((_, c) =>
            live.map((meci, i) => (
              <div key={`${c}-${meci.fixtureId}`} className="pr-4" aria-hidden={c > 0 || undefined}>
                <CardLive
                  meci={meci}
                  tinta={TINTE[i % TINTE.length]!}
                  clona={c > 0}
                  onOpen={() => onOpen(meci.fixtureId)}
                />
              </div>
            )),
          )}
        </div>
      </div>

      {depaseste && (
        <>
          <button
            type="button"
            aria-label="Derulează stânga"
            onClick={() => sare(-1)}
            className="absolute left-1.5 top-1/2 z-10 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full border border-line bg-card/90 text-ink2 shadow-card backdrop-blur transition hover:scale-105 hover:border-primary/40 hover:text-ink"
          >
            <IconChevronLeft width={16} height={16} />
          </button>
          <button
            type="button"
            aria-label="Derulează dreapta"
            onClick={() => sare(1)}
            className="absolute right-1.5 top-1/2 z-10 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full border border-line bg-card/90 text-ink2 shadow-card backdrop-blur transition hover:scale-105 hover:border-primary/40 hover:text-ink"
          >
            <IconChevronRight width={16} height={16} />
          </button>
        </>
      )}
    </div>
  );
}

function minutText(meci: MeciLive): string {
  if (meci.status === 'HT') return 'Pauză';
  if (meci.minut != null) return `${meci.minut}'`;
  return meci.status ?? 'LIVE';
}

/** Cat de avansat e meciul, 0..1 — coloreaza bara de desfasurare de sub card. */
function progres(meci: MeciLive): number {
  if (meci.status === 'HT') return 0.5;
  if (meci.status === 'ET' || meci.status === 'P') return 0.96;
  if (meci.minut == null) return 0.06;
  return Math.min(0.96, Math.max(0.06, meci.minut / 90));
}

function CardLive({
  meci,
  tinta,
  clona,
  onOpen,
}: {
  meci: MeciLive;
  tinta: string;
  clona: boolean;
  onOpen: () => void;
}) {
  const flash = useScoreFlash(`${meci.golGazde ?? 0}-${meci.golOaspeti ?? 0}`);
  const pct = Math.round(progres(meci) * 100);
  const liga = meci.ligaNume ?? (meci.leagueId != null ? numeLiga(meci.leagueId) : '');

  return (
    <button
      type="button"
      onClick={onOpen}
      tabIndex={clona ? -1 : 0}
      style={{ '--tinta': tinta } as CSSProperties}
      className="card-live-tinta group relative w-[200px] shrink-0 overflow-hidden rounded-xl border border-line/80 bg-card px-3 pb-2.5 pt-2 text-left transition duration-300 hover:-translate-y-[2px] hover:border-[rgb(var(--tinta)/0.55)] hover:shadow-cardHover"
    >
      <div className="mb-1.5 flex items-center gap-1.5">
        <span className="relative flex h-1.5 w-1.5 shrink-0">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-accent" />
        </span>
        <span className="text-[10px] font-extrabold tabular-nums text-accent">{minutText(meci)}</span>
        <span className="min-w-0 flex-1 truncate text-right text-[10px] font-medium text-ink2">{liga}</span>
      </div>

      <RandEchipa dto={meci.gazde} gol={meci.golGazde} flash={flash} />
      <RandEchipa dto={meci.oaspeti} gol={meci.golOaspeti} flash={flash} className="mt-1" />

      {/* bara de desfasurare: rosu = timpul scurs, verde = ce a mai ramas */}
      <div className="relative mt-2">
        <div className="flex h-[5px] overflow-hidden rounded-full bg-line/60">
          <div className="rounded-full bg-gradient-to-r from-accent/60 to-accent" style={{ width: `${pct}%` }} />
          <div className="flex-1 bg-win/40" />
        </div>
        <span
          className="absolute top-1/2 h-2 w-2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent shadow-[0_0_10px_2px_rgb(var(--gs-accent)/0.6)]"
          style={{ left: `${pct}%` }}
        />
      </div>
    </button>
  );
}

function RandEchipa({
  dto,
  gol,
  flash,
  className = '',
}: {
  dto: EchipaDto;
  gol: number | null;
  flash: boolean;
  className?: string;
}) {
  return (
    <span className={`flex items-center gap-2 ${className}`}>
      <TeamLogo nume={dto.nume} logo={dto.logo} size={18} />
      <span className="min-w-0 flex-1 truncate text-xs font-semibold text-ink">{numeEchipa(dto)}</span>
      <span
        className={`text-sm font-extrabold leading-none tabular-nums transition-[color,transform] duration-300 motion-reduce:transition-none ${
          flash ? 'scale-125 text-accent' : 'text-ink'
        }`}
      >
        {gol ?? 0}
      </span>
    </span>
  );
}
