import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { cauta } from '../../api/client';
import type { RezultatCautare, TipRezultat } from '../../api/types';
import { IconSearch, IconUser } from '../ui/icons';
import { TeamLogo } from '../ui/TeamLogo';
import { LigaLogo } from '../ui/LigaLogo';

const DEBOUNCE_MS = 300;
const MIN_CARACTERE = 2;

const ETICHETA_TIP: Record<TipRezultat, string> = {
  ECHIPA: 'Echipe',
  LIGA: 'Campionate',
  JUCATOR: 'Jucători',
};

/** Ruta paginii de destinatie pentru un rezultat, dupa tip. */
function ruta(r: RezultatCautare): string {
  switch (r.tip) {
    case 'ECHIPA':
      return `/echipa/${r.id}`;
    case 'LIGA':
      return `/competitie/${r.id}`;
    case 'JUCATOR':
      return `/jucator/${r.id}`;
  }
}

/** Iconita/avatarul unui rezultat, dupa tip. */
function Iconita({ r }: { r: RezultatCautare }) {
  if (r.tip === 'ECHIPA') {
    return <TeamLogo nume={r.nume} logo={r.imagine} size={24} />;
  }
  if (r.tip === 'LIGA') {
    return <LigaLogo id={r.id} logo={r.imagine} nume={r.nume} size={24} />;
  }
  return r.imagine ? (
    <img
      src={r.imagine}
      alt={r.nume ?? 'Jucător'}
      width={24}
      height={24}
      loading="lazy"
      className="h-6 w-6 shrink-0 rounded-full object-cover"
    />
  ) : (
    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary dark:bg-primary/20">
      <IconUser width={14} height={14} />
    </span>
  );
}

/** Search global din navbar: echipe + campionate + jucatori. Debounce, anulare cereri stale, navigare tastatura. */
export function Cautare() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [rezultate, setRezultate] = useState<RezultatCautare[]>([]);
  const [deschis, setDeschis] = useState(false);
  const [activ, setActiv] = useState(-1);
  const [seIncarca, setSeIncarca] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const termen = q.trim();

  useEffect(() => {
    if (termen.length < MIN_CARACTERE) {
      setRezultate([]);
      setSeIncarca(false);
      return;
    }
    const controller = new AbortController();
    setSeIncarca(true);
    const id = window.setTimeout(() => {
      cauta(termen, controller.signal)
        .then((r) => {
          setRezultate(r);
          setActiv(-1);
          setSeIncarca(false);
        })
        .catch((e) => {
          if (e instanceof DOMException && e.name === 'AbortError') return;
          setRezultate([]);
          setSeIncarca(false);
        });
    }, DEBOUNCE_MS);
    return () => {
      controller.abort();
      window.clearTimeout(id);
    };
  }, [termen]);

  useEffect(() => {
    function onClickAfara(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setDeschis(false);
      }
    }
    document.addEventListener('mousedown', onClickAfara);
    return () => document.removeEventListener('mousedown', onClickAfara);
  }, []);

  function selecteaza(r: RezultatCautare) {
    setDeschis(false);
    setQ('');
    setRezultate([]);
    navigate(ruta(r));
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (!deschis || rezultate.length === 0) {
      if (e.key === 'Escape') setDeschis(false);
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiv((i) => (i + 1) % rezultate.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiv((i) => (i - 1 + rezultate.length) % rezultate.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const ales = rezultate[activ >= 0 ? activ : 0];
      if (ales) selecteaza(ales);
    } else if (e.key === 'Escape') {
      setDeschis(false);
    }
  }

  const arataDropdown = deschis && termen.length >= MIN_CARACTERE;

  return (
    <div ref={containerRef} className="relative hidden lg:block">
      <IconSearch className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink2" width={16} height={16} />
      <input
        type="search"
        value={q}
        placeholder="Caută echipe, campionate, jucători…"
        role="combobox"
        aria-expanded={arataDropdown}
        aria-controls="cautare-lista"
        aria-autocomplete="list"
        onChange={(e) => {
          setQ(e.target.value);
          setDeschis(true);
        }}
        onFocus={() => setDeschis(true)}
        onKeyDown={onKeyDown}
        className="h-9 w-64 rounded-full border border-line bg-bg pl-9 pr-3 text-sm text-ink placeholder:text-ink2 focus:outline-none focus:ring-2 focus:ring-primary/40"
      />

      {arataDropdown && (
        <div
          id="cautare-lista"
          role="listbox"
          className="absolute left-0 right-0 top-11 z-30 max-h-96 overflow-y-auto rounded-xl border border-line bg-card py-1 shadow-lg"
        >
          {rezultate.length === 0 ? (
            <p className="px-3 py-3 text-sm text-ink2">
              {seIncarca ? 'Se caută…' : 'Niciun rezultat'}
            </p>
          ) : (
            rezultate.map((r, i) => {
              const antetNou = i === 0 || rezultate[i - 1]!.tip !== r.tip;
              return (
                <div key={`${r.tip}-${r.id}`}>
                  {antetNou && (
                    <p className="px-3 pb-1 pt-2 text-[11px] font-bold uppercase tracking-wide text-ink2">
                      {ETICHETA_TIP[r.tip]}
                    </p>
                  )}
                  <button
                    type="button"
                    role="option"
                    aria-selected={i === activ}
                    onMouseEnter={() => setActiv(i)}
                    onClick={() => selecteaza(r)}
                    className={`flex w-full items-center gap-3 px-3 py-2 text-left text-sm transition ${
                      i === activ ? 'bg-primary/10' : 'hover:bg-bg'
                    }`}
                  >
                    <Iconita r={r} />
                    <span className="min-w-0 flex-1 truncate font-semibold text-ink">{r.nume ?? '—'}</span>
                    {r.subtitlu && <span className="shrink-0 text-xs text-ink2">{r.subtitlu}</span>}
                  </button>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
