import { useState } from 'react';
import type { FiltruCompetitie } from '../../api/client';
import type {
  ArbitruDto,
  EchipaDto,
  FereastraFormaDto,
  FormaEchipaDto,
  FrecventaDto,
  LinieEchipaDto,
  PiataEchipaDto,
  PiataStatDto,
  StatisticiAvansateDto,
} from '../../api/types';
import { culoareProbabilitate } from '../ui/BaraProbabilitatePiata';
import { numeEchipa } from '../../lib/echipa';
import { formatRata } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { ComutatorCompetitie } from '../ui/ComutatorCompetitie';
import { Taburi, type Tab } from '../ui/Taburi';
import { TeamLogo } from '../ui/TeamLogo';

/**
 * Secțiunea „Statistici" a meciului, pe modelul aplicațiilor de statistici pe mobil: un ecran de
 * comparație (echipele față în față + șansele meciului), apoi câte un tab pe piață cu panoul fiecărei
 * echipe (ultimele 7 rezultate + liniile ei) și totalul meciului. Totul pe ultimele 7 meciuri:
 * gazdele acasă, oaspeții în deplasare.
 */

const TABURI: Tab[] = [
  { id: 'comparatie', eticheta: 'Comparație' },
  { id: 'goluri', eticheta: 'Goluri' },
  { id: 'cornere', eticheta: 'Cornere' },
  { id: 'cartonase', eticheta: 'Cartonașe' },
  { id: 'altele', eticheta: 'Egaluri, faulturi & șuturi' },
];

/** Sub atâtea meciuri în spate, cifra e doar orientativă. */
const PRAG_ESANTION_MIC = 6;

interface Echipe {
  gazde: EchipaDto;
  oaspeti: EchipaDto;
}

function procentDin(f: FrecventaDto | null | undefined): number | null {
  return f && f.total > 0 ? f.reusite / f.total : null;
}

function formatNumar(v: number | null | undefined): string {
  return v == null ? '—' : new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 }).format(v);
}

function frecventa(f: FrecventaDto): string {
  return f.total > 0 ? `${f.reusite}/${f.total}` : 'fără date';
}

/** ✓/✗ la meci terminat: modelul „zice da" de la 50% în sus. */
function Verdict({ rata, actual }: { rata: number; actual: boolean | null | undefined }) {
  if (actual == null) return null;
  const hit = (rata >= 0.5) === actual;
  return (
    <Badge variant={hit ? 'win' : 'loss'} className="shrink-0">
      {hit ? '✓' : '✗'} {actual ? 'Da' : 'Nu'}
    </Badge>
  );
}

/** O linie „Peste 2.5 goluri — 58%" cu bara colorată după prag (verde ≥70, galben 50–69, roșu <50). */
function LinieBara({
  eticheta,
  rata,
  detaliu,
  areDate = true,
  actual,
}: {
  eticheta: string;
  rata: number;
  detaliu?: string;
  areDate?: boolean;
  actual?: boolean | null;
}) {
  const valid = areDate && Number.isFinite(rata);
  const culoare = culoareProbabilitate(rata);
  return (
    <div className="py-2">
      <div className="flex items-center justify-between gap-2 text-sm">
        <span className="font-semibold text-ink">
          {eticheta} <span className={valid ? culoare.text : 'text-ink2'}>— {valid ? formatRata(rata) : '—'}</span>
        </span>
        <span className="flex items-center gap-2">
          {detaliu && <span className="text-right text-[11px] text-ink2">{detaliu}</span>}
          {valid && <Verdict rata={rata} actual={actual} />}
        </span>
      </div>
      <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-ink2/15">
        <div
          data-testid="umplere-bara"
          className={`h-full rounded-full ${valid ? culoare.bara : ''}`}
          style={{ width: valid ? `${Math.round(rata * 100)}%` : '0%' }}
        />
      </div>
    </div>
  );
}

/** Ultimele rezultate: chip verde = victorie, galben = egal, roșu = înfrângere (cel mai vechi primul). */
function UltimeleRezultate({ forma }: { forma: FereastraFormaDto | undefined }) {
  if (!forma || forma.meciuri.length === 0) {
    return <p className="text-xs italic text-ink2">Fără meciuri în fereastră.</p>;
  }
  const culoare = { V: 'bg-green-600', E: 'bg-yellow-500', I: 'bg-red-600' } as const;
  return (
    <div className="flex flex-wrap gap-1.5">
      {[...forma.meciuri].reverse().map((m, i) => (
        <span
          key={`${m.data}-${i}`}
          title={m.data}
          className={`rounded-md px-2 py-1 text-xs font-bold tabular-nums text-white ${culoare[m.rezultat]}`}
        >
          {m.golMarcate}-{m.golPrimite}
        </span>
      ))}
    </div>
  );
}

interface GrupLinii {
  titlu?: string;
  linii: LinieEchipaDto[];
  asteptat: number;
  real?: number | null;
}

/** Panoul unei echipe într-un tab de piață: sigla, ultimele 7 rezultate și liniile ei. */
function PanouEchipa({
  echipa,
  adversar,
  acasa,
  forma,
  grupuri,
  unitate,
}: {
  echipa: EchipaDto;
  adversar: EchipaDto;
  acasa: boolean;
  forma: FormaEchipaDto | undefined;
  grupuri: GrupLinii[];
  unitate: string;
}) {
  const loc = acasa ? 'acasă' : 'în deplasare';
  return (
    <Card className="p-5">
      <div className="flex flex-col items-center gap-2 text-center">
        <TeamLogo nume={echipa.nume} logo={echipa.logo} size={56} />
        <h3 className="text-lg font-extrabold text-ink">{numeEchipa(echipa)}</h3>
        <span className="text-xs font-semibold uppercase tracking-wide text-primary">{acasa ? 'Acasă' : 'Deplasare'}</span>
      </div>
      <p className="mt-4 text-sm font-bold text-ink">Ultimele 7 rezultate {loc}</p>
      <div className="mt-2">
        <UltimeleRezultate forma={forma?.locatie} />
      </div>
      {grupuri.map((g, i) => (
        <div key={g.titlu ?? i} className="mt-4">
          <div className="flex items-baseline justify-between text-xs text-ink2">
            <span className="font-semibold uppercase tracking-wide">{g.titlu ?? unitate}</span>
            <span>
              așteptat {formatNumar(g.asteptat)}
              {g.real != null && <span className="ml-1 font-semibold text-ink">· real {g.real}</span>}
            </span>
          </div>
          {g.linii.map((l) => {
            const n = l.facuteLocatie.total + l.primiteAdversarLocatie.total;
            return (
              <LinieBara
                key={l.linie}
                eticheta={`Peste ${l.linie} ${unitate}`}
                rata={l.probabilitate}
                areDate={n > 0}
                detaliu={`${frecventa(l.facuteLocatie)} ${loc} · ${numeEchipa(adversar)} a primit ${frecventa(
                  l.primiteAdversarLocatie,
                )}${n > 0 && n < PRAG_ESANTION_MIC ? ' · orientativ' : ''}`}
                actual={g.real != null ? g.real > l.linie : null}
              />
            );
          })}
        </div>
      ))}
    </Card>
  );
}

/** Totalul meciului pe o piață: barele modelului + frecvențele ambelor echipe pe scurt. */
function TotalMeci({
  titlu,
  piata,
  unitate,
  echipe,
  totalReal,
}: {
  titlu: string;
  piata: PiataStatDto | null | undefined;
  unitate: string;
  echipe: Echipe;
  totalReal?: number | null;
}) {
  if (!piata || piata.linii.length === 0) return null;
  return (
    <Card className="p-5">
      <div className="flex items-baseline justify-between gap-2">
        <h3 className="text-base font-bold text-ink">{titlu}</h3>
        {totalReal != null && <span className="text-xs font-semibold text-ink">Real: {totalReal}</span>}
      </div>
      <p className="text-[11px] text-ink2">
        {numeEchipa(echipe.gazde)}: {formatNumar(piata.gazde.totalLocatie)}/meci acasă · {numeEchipa(echipe.oaspeti)}:{' '}
        {formatNumar(piata.oaspeti.totalLocatie)}/meci în deplasare
      </p>
      {piata.linii.map((l) => {
        const n = l.gazdeLocatie.total + l.oaspetiLocatie.total;
        return (
          <LinieBara
            key={l.linie}
            eticheta={`Peste ${l.linie} ${unitate}`}
            rata={l.probabilitate}
            areDate={n > 0}
            detaliu={`${frecventa(l.gazdeLocatie)} · ${frecventa(l.oaspetiLocatie)}`}
            actual={totalReal != null ? totalReal > l.linie : null}
          />
        );
      })}
    </Card>
  );
}

/** Reprizele și egalurile: șansa pe meci + în câte meciuri s-a întâmplat la fiecare echipă. */
function ReprizeSiEgaluri({ s, echipe }: { s: StatisticiAvansateDto; echipe: Echipe }) {
  const r = s.rezultat;
  const fr = (a?: FrecventaDto, b?: FrecventaDto) =>
    a && b ? `${numeEchipa(echipe.gazde)} ${frecventa(a)} · ${numeEchipa(echipe.oaspeti)} ${frecventa(b)}` : undefined;
  const are = (a?: FrecventaDto, b?: FrecventaDto) => (a?.total ?? 0) + (b?.total ?? 0) > 0;
  const randuri = [
    { e: 'Ambele marchează', rata: s.gg.probabilitate, a: s.gg.gazdeMarcat, b: s.gg.oaspetiMarcat,
      actual: r?.ambeleMarcheaza },
    { e: 'Gol în repriza 1', rata: s.reprize?.golRepriza1, a: s.reprize?.repriza1Gazde,
      b: s.reprize?.repriza1Oaspeti, actual: r?.golRepriza1 },
    { e: 'Gol în repriza 2', rata: s.reprize?.golRepriza2, a: s.reprize?.repriza2Gazde,
      b: s.reprize?.repriza2Oaspeti, actual: r?.golRepriza2 },
  ];
  return (
    <Card className="p-5">
      <h3 className="text-base font-bold text-ink">Reprize și GG</h3>
      {randuri
        .filter((x) => x.rata != null)
        .map((x) => (
          <LinieBara key={x.e} eticheta={x.e} rata={x.rata as number} areDate={are(x.a, x.b)}
            detaliu={fr(x.a, x.b)} actual={x.actual} />
        ))}
    </Card>
  );
}

/** Egal la pauză / la final: șansa pe meci + în câte meciuri s-a întâmplat la fiecare echipă. */
function Egaluri({ s, echipe }: { s: StatisticiAvansateDto; echipe: Echipe }) {
  const e = s.egaluri;
  if (!e) return null;
  const r = s.rezultat;
  const fr = (a: FrecventaDto, b: FrecventaDto) =>
    `${numeEchipa(echipe.gazde)} ${frecventa(a)} · ${numeEchipa(echipe.oaspeti)} ${frecventa(b)}`;
  const are = e.pauzaGazde.total + e.pauzaOaspeti.total > 0;
  return (
    <Card className="p-5">
      <h3 className="text-base font-bold text-ink">Egaluri</h3>
      <LinieBara eticheta="Egal la pauză" rata={e.egalPauza} areDate={are} detaliu={fr(e.pauzaGazde, e.pauzaOaspeti)}
        actual={r?.egalPauza} />
      <LinieBara eticheta="Egal la final" rata={e.egalFinal} areDate={are} detaliu={fr(e.finalGazde, e.finalOaspeti)}
        actual={r?.egalFinal} />
    </Card>
  );
}

/** Pastila de procent din tabelul de comparație; culoarea după prag, intensitatea crește cu procentul. */
function PilulaProcent({ rata }: { rata: number | null }) {
  if (rata == null) return <span className="text-sm text-ink2">—</span>;
  return (
    <span
      className={`inline-block min-w-[4.5rem] rounded-lg px-2 py-1 text-center text-sm font-bold tabular-nums text-white ${
        culoareProbabilitate(rata).bara
      }`}
      style={{ opacity: 0.45 + 0.55 * rata }}
    >
      {formatRata(rata)}
    </span>
  );
}

function Comparatie({
  s,
  echipe,
  formaGazde,
  formaOaspeti,
}: {
  s: StatisticiAvansateDto;
  echipe: Echipe;
  formaGazde?: FormaEchipaDto;
  formaOaspeti?: FormaEchipaDto;
}) {
  const linie = (p: PiataStatDto, l: number) => p.linii.find((x) => x.linie === l);
  const g15 = linie(s.goluri, 1.5);
  const g25 = linie(s.goluri, 2.5);
  const c95 = linie(s.cornere, 9.5);
  const k45 = linie(s.cartonase, 4.5);
  const primite = (m: PiataStatDto['gazde']) =>
    m.totalLocatie != null && m.proprieLocatie != null ? m.totalLocatie - m.proprieLocatie : null;

  // „2,2 / meci · 11 total" — media si totalul pe fereastra (fara forma: doar media din statistici)
  const goluri = (f: FereastraFormaDto | undefined, marcate: boolean, medie: number | null) => {
    if (!f || f.meciuri.length === 0) return formatNumar(medie);
    const total = f.meciuri.reduce((acc, m) => acc + (marcate ? m.golMarcate : m.golPrimite), 0);
    return `${formatNumar(marcate ? f.goluriMarcatePeMeci : f.goluriPrimitePeMeci)} / meci · ${total} total`;
  };
  const numere: [string, string, string][] = [
    ['Goluri marcate (acasă / deplasare)', goluri(formaGazde?.locatie, true, s.goluri.gazde.proprieLocatie),
      goluri(formaOaspeti?.locatie, true, s.goluri.oaspeti.proprieLocatie)],
    ['Goluri primite (acasă / deplasare)', goluri(formaGazde?.locatie, false, primite(s.goluri.gazde)),
      goluri(formaOaspeti?.locatie, false, primite(s.goluri.oaspeti))],
    ['Goluri marcate (general)', goluri(formaGazde?.general, true, s.goluri.gazde.proprieGeneral),
      goluri(formaOaspeti?.general, true, s.goluri.oaspeti.proprieGeneral)],
    ['Goluri primite (general)', goluri(formaGazde?.general, false, null), goluri(formaOaspeti?.general, false, null)],
    ['Cornere / meci', formatNumar(s.cornere.gazde.proprieLocatie), formatNumar(s.cornere.oaspeti.proprieLocatie)],
    ['Cartonașe / meci', formatNumar(s.cartonase.gazde.proprieLocatie), formatNumar(s.cartonase.oaspeti.proprieLocatie)],
  ];
  const procente: [string, FrecventaDto | undefined, FrecventaDto | undefined][] = [
    ['A marcat', s.gg.gazdeMarcat, s.gg.oaspetiMarcat],
    ['A primit gol', s.gg.gazdePrimit, s.gg.oaspetiPrimit],
    ['Gol în repriza 1', s.reprize?.repriza1Gazde, s.reprize?.repriza1Oaspeti],
    ['Gol în repriza 2', s.reprize?.repriza2Gazde, s.reprize?.repriza2Oaspeti],
    ['Peste 1.5 goluri', g15?.gazdeLocatie, g15?.oaspetiLocatie],
    ['Peste 2.5 goluri', g25?.gazdeLocatie, g25?.oaspetiLocatie],
    ['Peste 9.5 cornere', c95?.gazdeLocatie, c95?.oaspetiLocatie],
    ['Peste 4.5 cartonașe', k45?.gazdeLocatie, k45?.oaspetiLocatie],
  ];

  const r = s.rezultat;
  const meci: { eticheta: string; rata: number | undefined; actual?: boolean | null }[] = [
    { eticheta: 'Peste 1.5 goluri', rata: g15?.probabilitate, actual: r ? r.totalGoluri > 1.5 : null },
    { eticheta: 'Peste 2.5 goluri', rata: g25?.probabilitate, actual: r ? r.totalGoluri > 2.5 : null },
    { eticheta: 'Ambele marchează', rata: s.gg.probabilitate, actual: r?.ambeleMarcheaza },
    { eticheta: 'Gol în repriza 1', rata: s.reprize?.golRepriza1, actual: r?.golRepriza1 },
    { eticheta: 'Gol în repriza 2', rata: s.reprize?.golRepriza2, actual: r?.golRepriza2 },
    {
      eticheta: 'Peste 9.5 cornere',
      rata: c95?.probabilitate,
      actual: r?.totalCornere != null ? r.totalCornere > 9.5 : null,
    },
    {
      eticheta: 'Peste 4.5 cartonașe',
      rata: k45?.probabilitate,
      actual: r?.totalCartonase != null ? r.totalCartonase > 4.5 : null,
    },
  ];

  const antetEchipa = (e: EchipaDto, loc: string) => (
    <div className="flex flex-col items-center gap-2">
      <TeamLogo nume={e.nume} logo={e.logo} size={48} />
      <span className="text-sm font-extrabold uppercase text-ink">{numeEchipa(e)}</span>
      <span className="text-[11px] text-ink2">{loc}</span>
    </div>
  );

  return (
    <Card className="p-5">
      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 text-center">
        {antetEchipa(echipe.gazde, 'ultimele 7 acasă')}
        <span className="text-xs font-semibold text-ink2">vs</span>
        {antetEchipa(echipe.oaspeti, 'ultimele 7 în deplasare')}
      </div>

      <div className="mt-5 border-t border-line pt-3">
        {numere.map(([eticheta, a, b]) => (
          <div key={eticheta} className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 py-1.5 text-center">
            <span className="text-sm font-bold tabular-nums text-ink">{a}</span>
            <span className="text-xs font-semibold text-ink2">{eticheta}</span>
            <span className="text-sm font-bold tabular-nums text-ink">{b}</span>
          </div>
        ))}
        {procente.map(([eticheta, a, b]) => (
          <div key={eticheta} className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 py-1.5 text-center">
            <span>
              <PilulaProcent rata={procentDin(a)} />
            </span>
            <span className="text-xs font-semibold text-ink2">{eticheta}</span>
            <span>
              <PilulaProcent rata={procentDin(b)} />
            </span>
          </div>
        ))}
      </div>

      <div className="mt-4 border-t border-line pt-3">
        <h3 className="text-sm font-bold text-ink">Șansele meciului</h3>
        {meci
          .filter((m) => m.rata != null)
          .map((m) => (
            <LinieBara key={m.eticheta} eticheta={m.eticheta} rata={m.rata as number} actual={m.actual} />
          ))}
      </div>
    </Card>
  );
}

function Arbitru({ arbitru }: { arbitru: ArbitruDto }) {
  const dif = Math.round((arbitru.factorCartonase - 1) * 100);
  const verdict =
    arbitru.meciuri === 0
      ? 'fără meciuri arbitrate în baza noastră'
      : Math.abs(dif) < 3
        ? 'în media ligii'
        : dif > 0
          ? `sever: +${dif}% la cartonașe`
          : `îngăduitor: ${dif}% la cartonașe`;
  return (
    <Card className="flex flex-wrap items-center justify-between gap-3 p-4">
      <div>
        <p className="text-sm font-bold text-ink">⚖️ Arbitru: {arbitru.nume}</p>
        <p className="text-xs text-ink2">
          {verdict}
          {arbitru.meciuri > 0 && arbitru.meciuri < PRAG_ESANTION_MIC && ' · cifră orientativă'}
        </p>
      </div>
      <div className="flex gap-4 text-center text-xs text-ink2">
        <span>
          <b className="block text-base text-ink">{formatNumar(arbitru.cartonasePeMeci)}</b>cartonașe/meci
        </span>
        <span>
          <b className="block text-base text-ink">{formatNumar(arbitru.cartonaseMediaLigii)}</b>media ligii
        </span>
        <span>
          <b className="block text-base text-ink">{arbitru.meciuri}</b>meciuri
        </span>
      </div>
    </Card>
  );
}

interface SectiuneStatisticiProps {
  statistici: StatisticiAvansateDto;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
  formaGazde?: FormaEchipaDto;
  formaOaspeti?: FormaEchipaDto;
  arbitru?: ArbitruDto | null;
  /** Filtrul de competiție; fără `onCompetitie` comutatorul nu se afișează. */
  competitie?: FiltruCompetitie;
  onCompetitie?: (valoare: FiltruCompetitie) => void;
  /** Tabul deschis la intrare (ex. „goluri" pentru verdictul meciului jucat). */
  piataInitiala?: string | null;
}

export function SectiuneStatistici({
  statistici: s,
  gazde,
  oaspeti,
  formaGazde,
  formaOaspeti,
  arbitru,
  competitie = 'toate',
  onCompetitie,
  piataInitiala,
}: SectiuneStatisticiProps) {
  const echipe: Echipe = { gazde, oaspeti };
  const r = s.rezultat;
  const real = r?.echipe;
  const [tab, setTab] = useState<string>(
    piataInitiala && TABURI.some((t) => t.id === piataInitiala) ? piataInitiala : 'comparatie',
  );

  const panouri = (
    piata: PiataEchipaDto | undefined,
    unitate: string,
    realGazde?: number | null,
    realOaspeti?: number | null,
    reprize = false,
  ) => {
    if (!piata) return null;
    const r1 = reprize ? s.cornereReprize?.echipeRepriza1 : undefined;
    const r2 = reprize ? s.cornereReprize?.echipeRepriza2 : undefined;
    const grupuri = (acasa: boolean): GrupLinii[] => [
      {
        linii: acasa ? piata.gazde : piata.oaspeti,
        asteptat: acasa ? piata.asteptareGazde : piata.asteptareOaspeti,
        real: acasa ? realGazde : realOaspeti,
      },
      ...(r1
        ? [{
            titlu: 'Repriza 1',
            linii: acasa ? r1.gazde : r1.oaspeti,
            asteptat: acasa ? r1.asteptareGazde : r1.asteptareOaspeti,
            real: acasa ? real?.cornereRepriza1Gazde : real?.cornereRepriza1Oaspeti,
          }]
        : []),
      ...(r2
        ? [{
            titlu: 'Repriza 2',
            linii: acasa ? r2.gazde : r2.oaspeti,
            asteptat: acasa ? r2.asteptareGazde : r2.asteptareOaspeti,
            real: acasa ? real?.cornereRepriza2Gazde : real?.cornereRepriza2Oaspeti,
          }]
        : []),
    ];
    return (
      <div className="grid gap-4 lg:grid-cols-2">
        <PanouEchipa echipa={gazde} adversar={oaspeti} acasa forma={formaGazde} grupuri={grupuri(true)}
          unitate={unitate} />
        <PanouEchipa echipa={oaspeti} adversar={gazde} acasa={false} forma={formaOaspeti} grupuri={grupuri(false)}
          unitate={unitate} />
      </div>
    );
  };

  const suma = (a?: number | null, b?: number | null) => (a != null && b != null ? a + b : null);

  function continut() {
    switch (tab) {
      case 'goluri':
        return (
          <>
            {panouri(s.echipe?.goluri, 'goluri', real?.goluriGazde, real?.goluriOaspeti)}
            <TotalMeci titlu="Goluri în meci" piata={s.goluri} unitate="goluri" echipe={echipe}
              totalReal={r?.totalGoluri} />
            <ReprizeSiEgaluri s={s} echipe={echipe} />
          </>
        );
      case 'cornere':
        return (
          <>
            {panouri(s.echipe?.cornere, 'cornere', real?.cornereGazde, real?.cornereOaspeti, true)}
            <TotalMeci titlu="Cornere în meci" piata={s.cornere} unitate="cornere" echipe={echipe}
              totalReal={r?.totalCornere} />
            <div className="grid gap-4 lg:grid-cols-2">
              <TotalMeci titlu="Cornere în repriza 1" piata={s.cornereReprize?.repriza1} unitate="cornere"
                echipe={echipe} totalReal={suma(real?.cornereRepriza1Gazde, real?.cornereRepriza1Oaspeti)} />
              <TotalMeci titlu="Cornere în repriza 2" piata={s.cornereReprize?.repriza2} unitate="cornere"
                echipe={echipe} totalReal={suma(real?.cornereRepriza2Gazde, real?.cornereRepriza2Oaspeti)} />
            </div>
          </>
        );
      case 'cartonase':
        return (
          <>
            {arbitru && <Arbitru arbitru={arbitru} />}
            {panouri(s.echipe?.cartonase, 'cartonașe', real?.cartonaseGazde, real?.cartonaseOaspeti)}
            <TotalMeci titlu="Cartonașe în meci" piata={s.cartonase} unitate="cartonașe" echipe={echipe}
              totalReal={r?.totalCartonase} />
          </>
        );
      case 'altele':
        return (
          <div className="grid gap-4 lg:grid-cols-2">
            <Egaluri s={s} echipe={echipe} />
            <TotalMeci titlu="Faulturi" piata={s.faulturi} unitate="faulturi" echipe={echipe}
              totalReal={r?.totalFaulturi} />
            <TotalMeci titlu="Șuturi" piata={s.suturi} unitate="șuturi" echipe={echipe} totalReal={r?.totalSuturi} />
            <TotalMeci titlu="Șuturi pe poartă" piata={s.suturiPePoarta} unitate="pe poartă" echipe={echipe}
              totalReal={r?.totalSuturiPePoarta} />
          </div>
        );
      default:
        return <Comparatie s={s} echipe={echipe} formaGazde={formaGazde} formaOaspeti={formaOaspeti} />;
    }
  }

  return (
    <section>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-extrabold text-ink">Statistici</h2>
        {onCompetitie && <ComutatorCompetitie valoare={competitie} onSchimba={onCompetitie} />}
      </div>
      <Taburi taburi={TABURI} activ={tab} onSchimba={setTab} className="mb-4" />
      <div key={tab} className="animate-fade-in space-y-4">
        {continut()}
      </div>
      <p className="mt-4 text-[11px] text-ink2">
        Ultimele 7 meciuri: {numeEchipa(gazde)} acasă, {numeEchipa(oaspeti)} în deplasare
        {competitie === 'curenta' ? ', doar din competiția acestui meci' : ''}. Procentul e șansa calculată pentru
        acest meci; la goluri fiecare gol e cântărit după forța adversarului. „4/7" = în câte meciuri s-a întâmplat.
      </p>
    </section>
  );
}
