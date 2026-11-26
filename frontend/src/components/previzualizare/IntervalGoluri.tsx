import { useState } from 'react';
import type { EchipaDto, IntervalGoluriDto } from '../../api/types';
import { numeEchipa } from '../../lib/echipa';
import { formatRata } from '../../lib/format';
import { culoareProbabilitate } from '../ui/BaraProbabilitatePiata';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { ChipSelect } from '../ui/ChipSelect';

/** Intervalul arătat la intrare: 1–3 goluri. */
const IMPLICIT: [number, number] = [1, 3];

/** P(între a și b goluri) = suma distribuției pe a..b. */
export function sansaInterval(distributie: number[], a: number, b: number): number {
  return distributie.slice(a, b + 1).reduce((s, p) => s + p, 0);
}

function inInterval(total: number, a: number, b: number): boolean {
  return total >= a && total <= b;
}

function frecventa(totaluri: number[], a: number, b: number): string {
  if (totaluri.length === 0) return 'fără date';
  return `${totaluri.filter((t) => inInterval(t, a, b)).length}/${totaluri.length}`;
}

/**
 * „Interval goluri”: alegi de la câte până la câte goluri, iar cardul arată șansa modelului, în câte
 * din ultimele meciuri ale fiecărei echipe s-a întâmplat și distribuția pe fiecare total (0…6, 7+).
 */
export function IntervalGoluri({
  interval,
  gazde,
  oaspeti,
  totalReal,
}: {
  interval: IntervalGoluriDto | null | undefined;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  totalReal?: number | null;
}) {
  const [[a, b], setLimite] = useState<[number, number]>(IMPLICIT);
  if (!interval || interval.distributie.length < 2) return null;

  const maxim = interval.distributie.length - 2; // ultima poziție = „mai multe”
  const rata = sansaInterval(interval.distributie, a, b);
  const celMaiMare = Math.max(...interval.distributie, 0.0001);
  const culoare = culoareProbabilitate(rata);
  const eticheta = a === b ? `Exact ${a} goluri` : `Între ${a} și ${b} goluri`;
  const actual = totalReal != null ? inInterval(totalReal, a, b) : null;
  const optiuni = (de: number, pana: number) =>
    Array.from({ length: pana - de + 1 }, (_, i) => ({ valoare: String(de + i), eticheta: String(de + i) }));

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-base font-bold text-ink">Interval goluri</h3>
        <div className="flex gap-2">
          <ChipSelect eticheta="De la" prefix="De la" latime="w-32" valoare={String(a)}
            onSchimba={(v) => setLimite([Number(v), b])} optiuni={optiuni(0, b)} />
          <ChipSelect eticheta="Până la" prefix="Până la" latime="w-32" valoare={String(b)}
            onSchimba={(v) => setLimite([a, Number(v)])} optiuni={optiuni(a, maxim)} />
        </div>
      </div>

      <div className="mt-3 flex items-center justify-between gap-2 text-sm">
        <span className="font-semibold text-ink">
          {eticheta} <span className={culoare.text}>— {formatRata(rata)}</span>
        </span>
        <span className="flex items-center gap-2">
          <span className="text-right text-[11px] text-ink2">
            {numeEchipa(gazde)} {frecventa(interval.totaluriGazde, a, b)} · {numeEchipa(oaspeti)}{' '}
            {frecventa(interval.totaluriOaspeti, a, b)}
          </span>
          {actual != null && (
            <Badge variant={(rata >= 0.5) === actual ? 'win' : 'loss'} className="shrink-0">
              {(rata >= 0.5) === actual ? '✓' : '✗'} {actual ? 'Da' : 'Nu'}
            </Badge>
          )}
        </span>
      </div>
      <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-ink2/15">
        <div className={`h-full rounded-full ${culoare.bara}`} style={{ width: `${Math.round(rata * 100)}%` }} />
      </div>

      <ol className="mt-4 grid grid-cols-8 gap-1.5" aria-label="Șansa pe fiecare total de goluri">
        {interval.distributie.map((p, k) => {
          const ales = k <= maxim && inInterval(k, a, b);
          return (
            <li key={k} className="flex flex-col items-center gap-1">
              <span className="text-[10px] tabular-nums text-ink2">{Math.round(p * 100)}%</span>
              <div className="flex h-14 w-full items-end overflow-hidden rounded-md bg-ink2/10">
                <div
                  className={`w-full rounded-md ${ales ? 'bg-piata' : 'bg-ink2/30'}`}
                  style={{ height: `${Math.max(4, Math.round((p / celMaiMare) * 100))}%` }}
                />
              </div>
              <span className={`text-[11px] font-semibold ${ales ? 'text-ink' : 'text-ink2'}`}>
                {k > maxim ? `${k}+` : k}
              </span>
            </li>
          );
        })}
      </ol>
    </Card>
  );
}
