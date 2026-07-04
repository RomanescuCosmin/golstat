import type { MeciForma } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatDataScurta } from '../../lib/format';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';

const VALOARE = { V: 2, E: 1, I: 0 } as const;
const CULOARE = { V: 'var(--color-win, #16a34a)', E: 'var(--color-draw, #d1a200)', I: 'var(--color-accent, #e23b3b)' } as const;

const W = 320;
const H = 96;
const PAD_X = 16;
const PAD_Y = 14;

/** Grafic de formă: line chart SVG hand-rolled (V=2/E=1/Î=0) pe rezultatele recente, cronologic. */
export function GraficForma({ rezultate }: { rezultate: MeciForma[] }) {
  // backend trimite cele mai recente primele — pentru grafic mergem cronologic (stânga = mai vechi)
  const puncte = [...rezultate].reverse();

  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-ink">Evoluția formei</h2>
      {puncte.length < 2 ? (
        <EmptyState titlu="Date insuficiente" mesaj="Nu există suficiente meciuri pentru un grafic." />
      ) : (
        <div className="mt-4">
          <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img" aria-label="Evoluția formei">
            {[0, 1, 2].map((nivel) => {
              const y = PAD_Y + (2 - nivel) * ((H - 2 * PAD_Y) / 2);
              return <line key={nivel} x1={PAD_X} y1={y} x2={W - PAD_X} y2={y} className="stroke-line" strokeWidth={1} />;
            })}
            <polyline
              fill="none"
              className="stroke-primary"
              strokeWidth={2}
              strokeLinejoin="round"
              strokeLinecap="round"
              points={puncte.map((m, i) => `${coordX(i, puncte.length)},${coordY(m.rezultat)}`).join(' ')}
            />
            {puncte.map((m, i) => (
              <circle
                key={m.fixtureId}
                cx={coordX(i, puncte.length)}
                cy={coordY(m.rezultat)}
                r={4}
                fill={CULOARE[m.rezultat]}
                stroke="var(--color-card, #fff)"
                strokeWidth={1.5}
              >
                <title>
                  {formatDataScurta(m.data)} · {numeEchipa(m.adversar)} · {m.golMarcate ?? '—'}-{m.golPrimite ?? '—'}
                </title>
              </circle>
            ))}
          </svg>
          <div className="mt-3 flex items-center justify-center gap-4 text-[11px] font-semibold text-ink2">
            <Legenda culoare={CULOARE.V} text="Victorie" />
            <Legenda culoare={CULOARE.E} text="Egal" />
            <Legenda culoare={CULOARE.I} text="Înfrângere" />
          </div>
        </div>
      )}
    </Card>
  );
}

function Legenda({ culoare, text }: { culoare: string; text: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: culoare }} />
      {text}
    </span>
  );
}

function coordX(i: number, total: number): number {
  return PAD_X + (total > 1 ? (i / (total - 1)) * (W - 2 * PAD_X) : 0);
}

function coordY(rezultat: 'V' | 'E' | 'I'): number {
  return PAD_Y + (2 - VALOARE[rezultat]) * ((H - 2 * PAD_Y) / 2);
}
