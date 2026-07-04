import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { cautaEchipe } from '../../api/client';
import type { RezultatCautare } from '../../api/types';
import { IconSearch } from '../ui/icons';
import { TeamLogo } from '../ui/TeamLogo';

const DEBOUNCE_MS = 300;
const MIN_CARACTERE = 2;

/** Search bar functional pentru echipe: debounce, anulare cereri stale, navigare tastatura. */
export function CautareEchipe() {
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
      cautaEchipe(termen, controller.signal)
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
    navigate(`/echipa/${r.teamId}`);
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
        placeholder="Caută echipe…"
        role="combobox"
        aria-expanded={arataDropdown}
        aria-controls="cautare-echipe-lista"
        aria-autocomplete="list"
        onChange={(e) => {
          setQ(e.target.value);
          setDeschis(true);
        }}
        onFocus={() => setDeschis(true)}
        onKeyDown={onKeyDown}
        className="h-9 w-56 rounded-full border border-line bg-bg pl-9 pr-3 text-sm text-ink placeholder:text-ink2 focus:outline-none focus:ring-2 focus:ring-primary/40"
      />

      {arataDropdown && (
        <div
          id="cautare-echipe-lista"
          role="listbox"
          className="absolute left-0 right-0 top-11 z-30 max-h-80 overflow-y-auto rounded-xl border border-line bg-card py-1 shadow-lg"
        >
          {rezultate.length === 0 ? (
            <p className="px-3 py-3 text-sm text-ink2">
              {seIncarca ? 'Se caută…' : 'Nicio echipă găsită'}
            </p>
          ) : (
            rezultate.map((r, i) => (
              <button
                key={r.teamId}
                type="button"
                role="option"
                aria-selected={i === activ}
                onMouseEnter={() => setActiv(i)}
                onClick={() => selecteaza(r)}
                className={`flex w-full items-center gap-3 px-3 py-2 text-left text-sm transition ${
                  i === activ ? 'bg-primary/10' : 'hover:bg-bg'
                }`}
              >
                <TeamLogo nume={r.nume} logo={r.logo} size={24} />
                <span className="min-w-0 flex-1 truncate font-semibold text-ink">{r.nume ?? '—'}</span>
                {r.tara && <span className="shrink-0 text-xs text-ink2">{r.tara}</span>}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
