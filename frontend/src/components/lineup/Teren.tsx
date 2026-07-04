import type { JucatorLineupDto } from '../../api/types';
import { IconUser } from '../ui/icons';

interface JucatorPozitionat {
  jucator: JucatorLineupDto;
  /** Procent 0..100 pe latimea terenului (0 = fundul propriu). */
  x: number;
  /** Procent 0..100 pe inaltimea terenului. */
  y: number;
}

/**
 * Aseaza titularii pe jumatatea lor de teren pornind de la `grid` "rand:coloana"
 * (rand 1 = linia portarului). Randul devine adancimea (x), coloana intinderea pe verticala (y).
 * Cand grid-ul lipseste, distribuim uniform pe linii dupa ordine.
 */
function pozitioneaza(titulari: JucatorLineupDto[], gazde: boolean): JucatorPozitionat[] {
  const cuGrid = titulari.filter((j) => j.grid && /^\d+:\d+$/.test(j.grid));
  const useGrid = cuGrid.length >= Math.ceil(titulari.length * 0.6);

  const parsat = titulari.map((j, i) => {
    let rand: number;
    let col: number;
    let coloanePeRand: number;
    if (useGrid && j.grid && /^\d+:\d+$/.test(j.grid)) {
      const [r, c] = j.grid.split(':').map(Number);
      rand = r;
      col = c;
      coloanePeRand = titulari.filter((o) => o.grid?.startsWith(`${r}:`)).length || 1;
    } else {
      rand = i === 0 ? 1 : Math.min(4, Math.floor((i - 1) / 3) + 2);
      col = i === 0 ? 1 : ((i - 1) % 3) + 1;
      coloanePeRand = 3;
    }
    return { j, rand, col, coloanePeRand };
  });

  const randuriMax = Math.max(...parsat.map((p) => p.rand), 1);

  return parsat.map(({ j, rand, col, coloanePeRand }) => {
    // adancime: rand 1 (portar) langa poarta proprie -> ~8%, ultimul rand -> ~46% (jumatate)
    const adancime = randuriMax > 1 ? ((rand - 1) / (randuriMax - 1)) * 38 + 8 : 8;
    const x = gazde ? adancime : 100 - adancime;
    const y = coloanePeRand > 1 ? ((col - 0.5) / coloanePeRand) * 100 : 50;
    return { jucator: j, x, y };
  });
}

function Jucator({ pozitie }: { pozitie: JucatorPozitionat }) {
  const { jucator, x, y } = pozitie;
  return (
    <div
      className="absolute flex -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-1"
      style={{ left: `${x}%`, top: `${y}%` }}
    >
      <span className="flex h-8 w-8 items-center justify-center rounded-full border border-white/60 bg-primary text-xs font-bold text-white shadow-card">
        {jucator.numar ?? <IconUser width={16} height={16} className="text-white" />}
      </span>
      <span className="max-w-[5.5rem] truncate rounded bg-card/80 px-1 text-[10px] font-semibold text-ink">
        {jucator.numar != null ? `${jucator.numar} ` : ''}
        {jucator.nume ?? '—'}
      </span>
    </div>
  );
}

interface TerenProps {
  gazde: JucatorLineupDto[];
  oaspeti: JucatorLineupDto[];
}

/** Terenul verde cu titularii ambelor echipe: gazdele pe jumatatea stanga, oaspetii pe dreapta (oglindit). */
export function Teren({ gazde, oaspeti }: TerenProps) {
  const pozGazde = pozitioneaza(gazde, true);
  const pozOaspeti = pozitioneaza(oaspeti, false);

  return (
    <div
      className="relative w-full overflow-hidden rounded-xl border border-win/30 bg-gradient-to-r from-win/15 via-win/10 to-win/15"
      style={{ aspectRatio: '16 / 9', minHeight: 280 }}
    >
      {/* linia si cercul de centru */}
      <div className="absolute inset-y-4 left-1/2 w-px -translate-x-1/2 bg-win/40" />
      <div className="absolute left-1/2 top-1/2 h-20 w-20 -translate-x-1/2 -translate-y-1/2 rounded-full border border-win/40" />
      {/* careuri */}
      <div className="absolute left-0 top-1/2 h-28 w-14 -translate-y-1/2 border-y border-r border-win/40" />
      <div className="absolute right-0 top-1/2 h-28 w-14 -translate-y-1/2 border-y border-l border-win/40" />

      {pozGazde.map((p, i) => (
        <Jucator key={`g-${p.jucator.id ?? i}`} pozitie={p} />
      ))}
      {pozOaspeti.map((p, i) => (
        <Jucator key={`o-${p.jucator.id ?? i}`} pozitie={p} />
      ))}
    </div>
  );
}
