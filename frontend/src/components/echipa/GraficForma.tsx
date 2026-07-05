import type { MeciForma } from '../../api/types';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

type Rezultat = 'V' | 'E' | 'I';

/** Fereastra rulanta peste care numaram V/E/I la fiecare etapa. */
const FEREASTRA = 5;

const NUME_SERIE: Record<Rezultat, string> = { V: 'Victorii', E: 'Egaluri', I: 'Înfrângeri' };

// Ordinea = ordinea de desenare (verde deasupra, ca in design).
const SERII: { rezultat: Rezultat; linie: string; punct: string }[] = [
  { rezultat: 'E', linie: 'stroke-draw', punct: 'fill-draw' },
  { rezultat: 'I', linie: 'stroke-accent', punct: 'fill-accent' },
  { rezultat: 'V', linie: 'stroke-win', punct: 'fill-win' },
];

const W = 360;
const H = 156;
const PAD_L = 26;
const PAD_R = 12;
const PAD_T = 12;
const PAD_B = 26;

interface Punct {
  x: number;
  y: number;
}

/** "Forma echipei": 3 linii netede (Victorii/Egaluri/Înfrângeri) — numar rulant pe ultimele 5 etape. */
export function GraficForma({ rezultate }: { rezultate: MeciForma[] }) {
  // backend trimite cele mai recente primele — pentru grafic mergem cronologic (stânga = mai vechi)
  const meciuri = [...rezultate].reverse();

  if (meciuri.length < 2) {
    return (
      <Card className="p-5">
        <h2 className="text-sm font-extrabold text-ink">Forma echipei</h2>
        <EmptyState titlu="Date insuficiente" mesaj="Nu există suficiente meciuri pentru un grafic." />
      </Card>
    );
  }

  const valori: Record<Rezultat, number[]> = { V: [], E: [], I: [] };
  meciuri.forEach((_, i) => {
    const fereastra = meciuri.slice(Math.max(0, i - FEREASTRA + 1), i + 1);
    for (const r of ['V', 'E', 'I'] as const) {
      valori[r].push(fereastra.filter((m) => m.rezultat === r).length);
    }
  });

  const maxY = Math.max(3, ...valori.V, ...valori.E, ...valori.I);
  const coordX = (i: number) => PAD_L + (i / (meciuri.length - 1)) * (W - PAD_L - PAD_R);
  const coordY = (v: number) => H - PAD_B - (v / maxY) * (H - PAD_T - PAD_B);

  const etichete = meciuri.map((m, i) => etichetaEtapa(m.runda, i));
  const pasEticheta = Math.max(1, Math.ceil(meciuri.length / 9));
  const niveluri = Array.from({ length: maxY + 1 }, (_, n) => n);

  return (
    <Card className="p-5">
      <h2 className="text-sm font-extrabold text-ink">Forma echipei</h2>

      <div className="mt-3 flex items-center gap-5 text-xs font-semibold text-ink2">
        <Legenda clasa="bg-win" text="Victorii" />
        <Legenda clasa="bg-draw" text="Egaluri" />
        <Legenda clasa="bg-accent" text="Înfrângeri" />
      </div>

      <div className="mt-3">
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img" aria-label="Forma echipei">
          {niveluri.map((nivel) => (
            <g key={nivel}>
              <line x1={PAD_L} y1={coordY(nivel)} x2={W - PAD_R} y2={coordY(nivel)} className="stroke-line" strokeWidth={1} />
              <text x={PAD_L - 8} y={coordY(nivel) + 3} textAnchor="end" fontSize={9} className="fill-ink2 font-medium">
                {nivel}
              </text>
            </g>
          ))}

          {etichete.map((eticheta, i) =>
            i % pasEticheta === 0 ? (
              <text key={meciuri[i]!.fixtureId} x={coordX(i)} y={H - 8} textAnchor="middle" fontSize={9.5} className="fill-ink2 font-medium">
                {eticheta}
              </text>
            ) : null,
          )}

          {SERII.map((serie) => {
            const puncte: Punct[] = valori[serie.rezultat].map((v, i) => ({ x: coordX(i), y: coordY(v) }));
            return (
              <g key={serie.rezultat}>
                <path
                  d={caleaNeteda(puncte)}
                  fill="none"
                  className={serie.linie}
                  strokeWidth={2}
                  strokeLinejoin="round"
                  strokeLinecap="round"
                />
                {puncte.map((p, i) => (
                  <circle key={meciuri[i]!.fixtureId} cx={p.x} cy={p.y} r={3.5} className={serie.punct}>
                    <title>
                      {etichete[i]} · {NUME_SERIE[serie.rezultat]}: {valori[serie.rezultat][i]}
                    </title>
                  </circle>
                ))}
              </g>
            );
          })}
        </svg>
      </div>
    </Card>
  );
}

function Legenda({ clasa, text }: { clasa: string; text: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className={`h-2.5 w-2.5 rounded-full ${clasa}`} />
      {text}
    </span>
  );
}

/** "Regular Season - 35" → "Et. 35"; fara numar de etapa cadem pe indexul meciului. */
function etichetaEtapa(runda: string | null, index: number): string {
  const nr = runda ? /(\d+)\s*$/.exec(runda) : null;
  return nr ? `Et. ${nr[1]}` : `${index + 1}`;
}

/** Cale SVG netedă (Catmull-Rom → Bézier cubic) prin puncte. */
function caleaNeteda(p: Punct[]): string {
  if (p.length < 2) return '';
  let d = `M ${p[0]!.x} ${p[0]!.y}`;
  for (let i = 0; i < p.length - 1; i++) {
    const p0 = p[i === 0 ? 0 : i - 1]!;
    const p1 = p[i]!;
    const p2 = p[i + 1]!;
    const p3 = p[i + 2 < p.length ? i + 2 : i + 1]!;
    const cp1x = p1.x + (p2.x - p0.x) / 6;
    const cp1y = p1.y + (p2.y - p0.y) / 6;
    const cp2x = p2.x - (p3.x - p1.x) / 6;
    const cp2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
  }
  return d;
}
