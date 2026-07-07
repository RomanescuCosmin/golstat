import { useState, type ReactNode } from 'react';
import type {
  EchipaDto,
  EgaluriDto,
  FrecventaDto,
  GgDto,
  LinieStatDto,
  MediiEchipaDto,
  PiataStatDto,
  ReprizeDto,
  RezultatStatisticiDto,
  StatisticiAvansateDto,
} from '../../api/types';
import { useGrowOnMount } from '../../hooks/useAnimatii';
import { numeEchipa } from '../../lib/echipa';
import { formatRata } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { Taburi, type Tab } from '../ui/Taburi';

/**
 * Badge ✓/✗ la meciuri terminate: `actual` = s-a intamplat evenimentul (ex. peste linie); modelul
 * favorizeaza „da" cand `rata >= 0.5`. Nimeresc cand partea favorizata coincide cu realitatea.
 */
function RezultatBadge({
  rata,
  actual,
  etichetaDa,
  etichetaNu,
}: {
  rata: number;
  actual: boolean;
  etichetaDa: string;
  etichetaNu: string;
}) {
  const hit = (rata >= 0.5) === actual;
  return (
    <Badge variant={hit ? 'win' : 'loss'} className="shrink-0">
      {hit ? '✓' : '✗'} {actual ? etichetaDa : etichetaNu}
    </Badge>
  );
}

/**
 * Secțiunea centrală „Statistici": analiza pe piețe a meciului, pe ultimele 7 meciuri —
 * gazdele ACASĂ și oaspeții în DEPLASARE — cu probabilitatea modelată per linie (bară custom)
 * și frecvențele empirice per echipă (banda de 7 puncte), plus legenda explicită.
 */

/** Bară de probabilitate custom: umplere gradient + marker la 50%. Fără istoric rămâne goală. */
function BaraProbabilitate({ rata, areIstoric = true }: { rata: number; areIstoric?: boolean }) {
  const montat = useGrowOnMount();
  const procent = Number.isFinite(rata) ? Math.max(0, Math.min(100, rata * 100)) : 0;
  const puternica = procent >= 50;
  return (
    <div className="relative h-3 flex-1 overflow-hidden rounded-full bg-ink2/10 dark:bg-ink2/15">
      {areIstoric && (
        <div
          className={`h-full rounded-full bg-gradient-to-r transition-[width] duration-700 ease-out motion-reduce:transition-none ${
            puternica ? 'from-primary/60 to-primary' : 'from-draw/60 to-draw'
          }`}
          style={{ width: montat ? `${procent}%` : '0%' }}
        />
      )}
      <span className="absolute inset-y-0 left-1/2 w-px -translate-x-1/2 bg-ink2/30" aria-hidden />
    </div>
  );
}

/** Banda celor (max) 7 meciuri: puncte pline = reușite. */
function PuncteFrecventa({ frecventa, plin }: { frecventa: FrecventaDto; plin: string }) {
  if (frecventa.total === 0) {
    return <span className="text-[11px] italic text-ink2">fără date</span>;
  }
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className="inline-flex gap-[3px]">
        {Array.from({ length: frecventa.total }, (_, i) => (
          <span
            key={i}
            className={`h-2 w-2 rounded-full ${i < frecventa.reusite ? plin : 'bg-ink2/20'}`}
          />
        ))}
      </span>
      <span className="text-[11px] font-bold tabular-nums text-ink">
        {frecventa.reusite}/{frecventa.total}
      </span>
    </span>
  );
}

interface ContextEchipe {
  gazde: EchipaDto;
  oaspeti: EchipaDto;
}

/** Un rând de piață: eticheta + bara modelată + frecvențele ambelor echipe cu legendă. */
function RandLinie({
  eticheta,
  rata,
  gazdeLocatie,
  gazdeGeneral,
  oaspetiLocatie,
  oaspetiGeneral,
  descriere,
  echipe,
  actual,
  etichetaDa = 'Peste',
  etichetaNu = 'Sub',
}: {
  eticheta: string;
  rata: number;
  gazdeLocatie: FrecventaDto;
  gazdeGeneral?: FrecventaDto;
  oaspetiLocatie: FrecventaDto;
  oaspetiGeneral?: FrecventaDto;
  /** ex. "peste 9.5 cornere" — intră în legenda "în X/7 meciuri acasă". */
  descriere: string;
  echipe: ContextEchipe;
  /** La meci terminat: s-a produs evenimentul (peste linie / egal / gol)? `null`/undefined = fără badge. */
  actual?: boolean | null;
  etichetaDa?: string;
  etichetaNu?: string;
}) {
  const areIstoric = gazdeLocatie.total > 0 || oaspetiLocatie.total > 0;
  return (
    <div className="border-b border-line py-3 last:border-b-0 last:pb-0">
      <div className="flex items-center gap-3">
        <span className="w-24 shrink-0 text-sm font-semibold text-ink">{eticheta}</span>
        <BaraProbabilitate rata={rata} areIstoric={areIstoric} />
        <span
          className={`w-12 shrink-0 text-right text-base font-extrabold tabular-nums ${
            rata >= 0.5 ? 'text-primary' : 'text-draw'
          }`}
        >
          {formatRata(rata)}
        </span>
        {actual != null && (
          <RezultatBadge rata={rata} actual={actual} etichetaDa={etichetaDa} etichetaNu={etichetaNu} />
        )}
      </div>
      <div className="mt-2 grid gap-x-6 gap-y-1 sm:grid-cols-2">
        <div className="flex min-w-0 items-center justify-between gap-2">
          <span className="truncate text-[11px] text-ink2" title={numeEchipa(echipe.gazde) ?? undefined}>
            {numeEchipa(echipe.gazde)} · acasă
          </span>
          <PuncteFrecventa frecventa={gazdeLocatie} plin="bg-primary" />
        </div>
        <div className="flex min-w-0 items-center justify-between gap-2">
          <span className="truncate text-[11px] text-ink2" title={numeEchipa(echipe.oaspeti) ?? undefined}>
            {numeEchipa(echipe.oaspeti)} · deplasare
          </span>
          <PuncteFrecventa frecventa={oaspetiLocatie} plin="bg-accent" />
        </div>
      </div>
      <p className="mt-1.5 text-[11px] leading-snug text-ink2">
        {legendaLinie(descriere, gazdeLocatie, gazdeGeneral, oaspetiLocatie, oaspetiGeneral, echipe)}
      </p>
    </div>
  );
}

function legendaLinie(
  descriere: string,
  gazdeLocatie: FrecventaDto,
  gazdeGeneral: FrecventaDto | undefined,
  oaspetiLocatie: FrecventaDto,
  oaspetiGeneral: FrecventaDto | undefined,
  echipe: ContextEchipe,
): string {
  const parti: string[] = [];
  if (gazdeLocatie.total > 0) {
    let p = `${numeEchipa(echipe.gazde)}: ${descriere} în ${gazdeLocatie.reusite}/${gazdeLocatie.total} meciuri acasă`;
    if (gazdeGeneral && gazdeGeneral.total > 0) {
      p += ` (${gazdeGeneral.reusite}/${gazdeGeneral.total} în general)`;
    }
    parti.push(p);
  }
  if (oaspetiLocatie.total > 0) {
    let p = `${numeEchipa(echipe.oaspeti)}: ${oaspetiLocatie.reusite}/${oaspetiLocatie.total} în deplasare`;
    if (oaspetiGeneral && oaspetiGeneral.total > 0) {
      p += ` (${oaspetiGeneral.reusite}/${oaspetiGeneral.total} în general)`;
    }
    parti.push(p);
  }
  return parti.length > 0 ? parti.join(' · ') : 'Fără istoric — probabilitatea vine din media ligii.';
}

function formatMedie(v: number | null): string {
  return v == null ? '—' : new Intl.NumberFormat('ro-RO', { maximumFractionDigits: 1 }).format(v);
}

/** Antetul unui card de piață: mediile pe meci ale fiecărei echipe, pe fereastra ei. */
function MediiPiata({ medii, echipe, unitate }: { medii: PiataStatDto; echipe: ContextEchipe; unitate: string }) {
  const rand = (nume: string | null, m: MediiEchipaDto, locatie: string) =>
    m.proprieLocatie == null && m.totalLocatie == null ? null : (
      <p key={locatie} className="text-[11px] text-ink2">
        <span className="font-semibold text-ink">{nume}</span> ({locatie}): {formatMedie(m.proprieLocatie)}{' '}
        {unitate}/meci · total meci {formatMedie(m.totalLocatie)}
        {m.proprieGeneral != null && ` · general ${formatMedie(m.proprieGeneral)}/meci`}
      </p>
    );
  return (
    <div className="mt-1 space-y-0.5">
      {rand(numeEchipa(echipe.gazde), medii.gazde, 'acasă')}
      {rand(numeEchipa(echipe.oaspeti), medii.oaspeti, 'deplasare')}
    </div>
  );
}

/** Card generic pentru o piață cu linii Over/Under. `totalReal` = totalul meciului la meci terminat. */
function CardPiata({
  titlu,
  unitate,
  piata,
  echipe,
  totalReal,
  children,
}: {
  titlu: string;
  unitate: string;
  piata: PiataStatDto;
  echipe: ContextEchipe;
  totalReal?: number | null;
  children?: ReactNode;
}) {
  return (
    <Card className="h-full p-5">
      <div className="flex items-start justify-between gap-3">
        <h3 className="text-base font-bold text-ink">{titlu}</h3>
        {totalReal != null && (
          <span className="shrink-0 rounded-md bg-ink2/10 px-2 py-0.5 text-xs font-semibold text-ink dark:bg-ink2/15">
            Rezultat real: {totalReal} {unitate}
          </span>
        )}
      </div>
      <MediiPiata medii={piata} echipe={echipe} unitate={unitate} />
      <div className="mt-2">
        {piata.linii.map((linie: LinieStatDto) => (
          <RandLinie
            key={linie.linie}
            eticheta={`Peste ${linie.linie}`}
            rata={linie.probabilitate}
            gazdeLocatie={linie.gazdeLocatie}
            gazdeGeneral={linie.gazdeGeneral}
            oaspetiLocatie={linie.oaspetiLocatie}
            oaspetiGeneral={linie.oaspetiGeneral}
            descriere={`peste ${linie.linie} ${unitate}`}
            echipe={echipe}
            actual={totalReal != null ? totalReal > linie.linie : null}
          />
        ))}
        {children}
      </div>
    </Card>
  );
}

/** Rândul GG din cardul de goluri, cu legenda marcat/primit. */
function RandGg({ gg, echipe, actual }: { gg: GgDto; echipe: ContextEchipe; actual?: boolean | null }) {
  const areIstoric = gg.gazdeMarcat.total > 0 || gg.oaspetiMarcat.total > 0;
  return (
    <div className="pt-3">
      <div className="flex items-center gap-3">
        <span className="w-24 shrink-0 text-sm font-semibold text-ink">GG (ambele)</span>
        <BaraProbabilitate rata={gg.probabilitate} areIstoric={areIstoric} />
        <span
          className={`w-12 shrink-0 text-right text-base font-extrabold tabular-nums ${
            gg.probabilitate >= 0.5 ? 'text-primary' : 'text-draw'
          }`}
        >
          {formatRata(gg.probabilitate)}
        </span>
        {actual != null && (
          <RezultatBadge rata={gg.probabilitate} actual={actual} etichetaDa="Da" etichetaNu="Nu" />
        )}
      </div>
      <p className="mt-1.5 text-[11px] leading-snug text-ink2">
        {gg.gazdeMarcat.total > 0 &&
          `${numeEchipa(echipe.gazde)}: a marcat în ${gg.gazdeMarcat.reusite}/${gg.gazdeMarcat.total} și a primit în ${gg.gazdePrimit.reusite}/${gg.gazdePrimit.total} meciuri acasă`}
        {gg.gazdeMarcat.total > 0 && gg.oaspetiMarcat.total > 0 && ' · '}
        {gg.oaspetiMarcat.total > 0 &&
          `${numeEchipa(echipe.oaspeti)}: a marcat în ${gg.oaspetiMarcat.reusite}/${gg.oaspetiMarcat.total} și a primit în ${gg.oaspetiPrimit.reusite}/${gg.oaspetiPrimit.total} în deplasare`}
        {gg.gazdeMarcat.total === 0 && gg.oaspetiMarcat.total === 0 &&
          'Fără istoric — probabilitatea vine din media ligii.'}
      </p>
    </div>
  );
}

/** Cardul de sine stătător „Ambele echipe marchează" (folosit ca sub-tab separat). */
function CardGg({ gg, echipe, actual }: { gg: GgDto; echipe: ContextEchipe; actual?: boolean | null }) {
  return (
    <Card className="p-5">
      <h3 className="text-base font-bold text-ink">Ambele echipe marchează</h3>
      <p className="mt-1 text-[11px] text-ink2">
        Probabilitatea ca ambele echipe să înscrie, din formă + model, regresată spre media ligii.
      </p>
      <div className="mt-1">
        <RandGg gg={gg} echipe={echipe} actual={actual} />
      </div>
    </Card>
  );
}

/** Cardul „Egaluri & reprize": egal la pauză/final + gol în repriza 1/2. */
function CardEgaluriReprize({
  egaluri,
  reprize,
  echipe,
  rezultat,
}: {
  egaluri: EgaluriDto | null;
  reprize: ReprizeDto | null;
  echipe: ContextEchipe;
  rezultat: RezultatStatisticiDto | null;
}) {
  if (!egaluri && !reprize) return null;
  return (
    <Card className="h-full p-5">
      <h3 className="text-base font-bold text-ink">Egaluri & reprize</h3>
      <div className="mt-2">
        {egaluri && (
          <>
            <RandLinie
              eticheta="Egal pauză"
              rata={egaluri.egalPauza}
              gazdeLocatie={egaluri.pauzaGazde}
              oaspetiLocatie={egaluri.pauzaOaspeti}
              descriere="egal la pauză"
              echipe={echipe}
              actual={rezultat?.egalPauza}
              etichetaDa="Egal"
              etichetaNu="Fără egal"
            />
            <RandLinie
              eticheta="Egal final"
              rata={egaluri.egalFinal}
              gazdeLocatie={egaluri.finalGazde}
              oaspetiLocatie={egaluri.finalOaspeti}
              descriere="egal la final"
              echipe={echipe}
              actual={rezultat != null ? rezultat.egalFinal : null}
              etichetaDa="Egal"
              etichetaNu="Fără egal"
            />
          </>
        )}
        {reprize && (
          <>
            <RandLinie
              eticheta="Gol repriza 1"
              rata={reprize.golRepriza1}
              gazdeLocatie={reprize.repriza1Gazde}
              oaspetiLocatie={reprize.repriza1Oaspeti}
              descriere="gol în prima repriză"
              echipe={echipe}
              actual={rezultat?.golRepriza1}
              etichetaDa="Gol"
              etichetaNu="Fără gol"
            />
            <RandLinie
              eticheta="Gol repriza 2"
              rata={reprize.golRepriza2}
              gazdeLocatie={reprize.repriza2Gazde}
              oaspetiLocatie={reprize.repriza2Oaspeti}
              descriere="gol în a doua repriză"
              echipe={echipe}
              actual={rezultat?.golRepriza2}
              etichetaDa="Gol"
              etichetaNu="Fără gol"
            />
          </>
        )}
      </div>
    </Card>
  );
}

interface SectiuneStatisticiProps {
  statistici: StatisticiAvansateDto;
  gazde: EchipaDto;
  oaspeti: EchipaDto;
}

const SUBTABURI: Tab[] = [
  { id: 'goluri', eticheta: 'Goluri' },
  { id: 'egaluri', eticheta: 'Pauză / final egal' },
  { id: 'gg', eticheta: 'Ambele marchează' },
  { id: 'cornere', eticheta: 'Cornere' },
  { id: 'cartonase', eticheta: 'Cartonașe' },
  { id: 'faulturi', eticheta: 'Faulturi' },
  { id: 'suturi', eticheta: 'Șuturi' },
  { id: 'suturiPePoarta', eticheta: 'Șuturi pe poartă' },
];

export function SectiuneStatistici({ statistici, gazde, oaspeti }: SectiuneStatisticiProps) {
  const echipe: ContextEchipe = { gazde, oaspeti };
  const rezultat = statistici.rezultat;
  const [subTab, setSubTab] = useState<string>('goluri');

  function continut() {
    switch (subTab) {
      case 'goluri':
        return (
          <CardPiata titlu="Goluri" unitate="goluri" piata={statistici.goluri} echipe={echipe}
            totalReal={rezultat?.totalGoluri} />
        );
      case 'egaluri':
        return statistici.egaluri || statistici.reprize ? (
          <CardEgaluriReprize egaluri={statistici.egaluri} reprize={statistici.reprize} echipe={echipe}
            rezultat={rezultat} />
        ) : (
          <Card className="p-5">
            <h3 className="text-base font-bold text-ink">Pauză / final egal</h3>
            <EmptyState titlu="Fără date" mesaj="Nu există istoric de reprize pentru aceste echipe." />
          </Card>
        );
      case 'gg':
        return <CardGg gg={statistici.gg} echipe={echipe} actual={rezultat?.ambeleMarcheaza} />;
      case 'cornere':
        return (
          <CardPiata titlu="Cornere" unitate="cornere" piata={statistici.cornere} echipe={echipe}
            totalReal={rezultat?.totalCornere} />
        );
      case 'cartonase':
        return (
          <CardPiata titlu="Cartonașe" unitate="cartonașe" piata={statistici.cartonase} echipe={echipe}
            totalReal={rezultat?.totalCartonase} />
        );
      case 'faulturi':
        return (
          <CardPiata titlu="Faulturi" unitate="faulturi" piata={statistici.faulturi} echipe={echipe}
            totalReal={rezultat?.totalFaulturi} />
        );
      case 'suturi':
        return (
          <CardPiata titlu="Șuturi" unitate="șuturi" piata={statistici.suturi} echipe={echipe}
            totalReal={rezultat?.totalSuturi} />
        );
      case 'suturiPePoarta':
        return (
          <CardPiata titlu="Șuturi pe poartă" unitate="șuturi pe poartă" piata={statistici.suturiPePoarta}
            echipe={echipe} totalReal={rezultat?.totalSuturiPePoarta} />
        );
      default:
        return null;
    }
  }

  return (
    <section>
      <div className="mb-3 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h2 className="text-lg font-extrabold text-ink">Statistici</h2>
        <p className="text-xs text-ink2">
          Ultimele 7 meciuri: {numeEchipa(gazde)} acasă · {numeEchipa(oaspeti)} în deplasare.
          Procentul = probabilitatea modelată pentru acest meci (formă + model de distribuție,
          regresat spre media ligii); punctele = frecvența empirică.
        </p>
      </div>

      <Taburi taburi={SUBTABURI} activ={subTab} onSchimba={setSubTab} varianta="pilule" className="mb-4" />

      <div key={subTab} className="animate-fade-in">
        {continut()}
      </div>
    </section>
  );
}
