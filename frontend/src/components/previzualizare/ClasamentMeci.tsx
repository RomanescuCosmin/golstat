import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getClasament } from '../../api/client';
import type { ClasamentDetaliat, MeciFormaClasament, RandClasamentDetaliat } from '../../api/types';
import { formatDataScurta } from '../../lib/format';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { Spinner } from '../ui/Spinner';
import { TeamLogo } from '../ui/TeamLogo';

/** Culori pentru zonele de clasament, în ordinea apariției; retrogradarea e mereu roșie. */
const CULORI_ZONE = ['bg-blue-600', 'bg-orange-500', 'bg-cyan-500', 'bg-violet-500', 'bg-emerald-500'];
const CULOARE_FORMA = { V: 'bg-win', E: 'bg-draw', I: 'bg-accent' } as const;

function culoriZone(randuri: RandClasamentDetaliat[]): Map<string, string> {
  const out = new Map<string, string>();
  let i = 0;
  for (const r of randuri) {
    if (!r.zona || out.has(r.zona)) continue;
    out.set(r.zona, /^relegation$|retrogradare$/i.test(r.zona) ? 'bg-red-600' : CULORI_ZONE[i++ % CULORI_ZONE.length]);
  }
  return out;
}

const nr = (v: number | null) => (v == null ? '—' : v);

function ChipForma({ m }: { m: MeciFormaClasament }) {
  const titlu = `${m.data ? formatDataScurta(m.data) + ' · ' : ''}${m.acasa ? 'vs' : 'la'} ${m.adversar ?? '?'} · ${m.golGazde}-${m.golOaspeti}`;
  return (
    <Link
      to={`/meci/${m.fixtureId}/centru`}
      title={titlu}
      className={`flex h-5 w-5 items-center justify-center rounded text-[10px] font-bold text-white transition hover:opacity-80 ${CULOARE_FORMA[m.rezultat]}`}
    >
      {m.rezultat}
    </Link>
  );
}

interface ClasamentMeciProps {
  leagueId: number;
  sezon: number;
  gazdeId: number;
  oaspetiId: number;
}

/**
 * Clasamentul oficial al competiției, pe modelul Flashscore: poziție cu zona colorată, MJ, V, E, Î,
 * goluri, golaveraj, puncte și forma pe ultimele 5; echipele meciului evidențiate. La competițiile cu
 * grupe se deschide grupa meciului.
 */
export function ClasamentMeci({ leagueId, sezon, gazdeId, oaspetiId }: ClasamentMeciProps) {
  const [date, setDate] = useState<ClasamentDetaliat | null>(null);
  const [eroare, setEroare] = useState(false);
  const [indexGrupa, setIndexGrupa] = useState(0);

  useEffect(() => {
    let anulat = false;
    setEroare(false);
    getClasament(leagueId, sezon)
      .then((d) => {
        if (anulat) return;
        const aMeciului = d.grupe.findIndex((g) =>
          g.randuri.some((r) => r.teamId === gazdeId || r.teamId === oaspetiId),
        );
        setIndexGrupa(Math.max(0, aMeciului));
        setDate(d);
      })
      .catch(() => !anulat && setEroare(true));
    return () => {
      anulat = true;
    };
  }, [leagueId, sezon, gazdeId, oaspetiId]);

  const grupa = date?.grupe[indexGrupa];
  const zone = useMemo(() => culoriZone(grupa?.randuri ?? []), [grupa]);

  if (eroare) {
    return (
      <Card className="p-5">
        <EmptyState titlu="Clasament indisponibil" mesaj="Nu am putut încărca clasamentul competiției." />
      </Card>
    );
  }
  if (!date) {
    return (
      <div className="flex justify-center py-10">
        <Spinner size={28} />
      </div>
    );
  }
  if (!grupa || grupa.randuri.length === 0) {
    return (
      <Card className="p-5">
        <EmptyState titlu="Fără clasament" mesaj="Competiția nu are încă un clasament publicat." />
      </Card>
    );
  }

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {date.grupe.length > 1 ? (
          <select
            aria-label="Grupa"
            value={indexGrupa}
            onChange={(e) => setIndexGrupa(Number(e.target.value))}
            className="rounded-lg border border-line bg-card px-2 py-1.5 text-sm font-semibold text-ink"
          >
            {date.grupe.map((g, i) => (
              <option key={g.nume ?? i} value={i}>
                {g.nume}
              </option>
            ))}
          </select>
        ) : (
          <h2 className="text-base font-bold text-ink">Clasament</h2>
        )}
        <Link to={`/competitie/${leagueId}`} className="text-xs font-semibold text-primary hover:opacity-70">
          Vezi competiția
        </Link>
      </div>

      <div className="mt-3 overflow-x-auto">
        <table className="w-full min-w-[22rem] text-sm">
          <thead>
            <tr className="border-b border-line text-[11px] font-semibold uppercase tracking-wide text-ink2">
              <th className="w-8 py-2 text-left">#</th>
              <th className="py-2 text-left">Echipa</th>
              <th className="w-9 py-2 text-center" title="Meciuri jucate">MJ</th>
              <th className="hidden w-8 py-2 text-center sm:table-cell" title="Victorii">V</th>
              <th className="hidden w-8 py-2 text-center sm:table-cell" title="Egaluri">E</th>
              <th className="hidden w-8 py-2 text-center sm:table-cell" title="Înfrângeri">Î</th>
              <th className="w-14 py-2 text-center" title="Goluri marcate : primite">G</th>
              <th className="w-10 py-2 text-center" title="Golaveraj">DG</th>
              <th className="w-10 py-2 text-center" title="Puncte">PCT</th>
              <th className="hidden w-32 py-2 text-center md:table-cell">Formă</th>
            </tr>
          </thead>
          <tbody>
            {grupa.randuri.map((r) => {
              const aMeciului = r.teamId === gazdeId || r.teamId === oaspetiId;
              const zona = r.zona ? zone.get(r.zona) : undefined;
              const dg = r.golaveraj;
              return (
                <tr
                  key={r.teamId}
                  className={`border-b border-line/60 ${aMeciului ? 'bg-primary/10 font-semibold' : 'hover:bg-bg/60'}`}
                >
                  <td className="py-1.5">
                    <span
                      title={r.zona ?? undefined}
                      className={`flex h-6 w-6 items-center justify-center rounded text-xs font-bold tabular-nums ${
                        zona ? `${zona} text-white` : 'text-ink2'
                      }`}
                    >
                      {r.pozitie}
                    </span>
                  </td>
                  <td className="py-1.5">
                    <Link to={`/echipa/${r.teamId}`} className="flex min-w-0 items-center gap-2 hover:underline">
                      <TeamLogo nume={r.nume} logo={r.logo} size={20} className="shrink-0" />
                      <span className={`truncate ${aMeciului ? 'text-primary' : 'text-ink'}`}>{r.nume ?? `#${r.teamId}`}</span>
                    </Link>
                  </td>
                  <td className="py-1.5 text-center tabular-nums text-ink">{nr(r.jucate)}</td>
                  <td className="hidden py-1.5 text-center tabular-nums text-ink sm:table-cell">{nr(r.victorii)}</td>
                  <td className="hidden py-1.5 text-center tabular-nums text-ink sm:table-cell">{nr(r.egaluri)}</td>
                  <td className="hidden py-1.5 text-center tabular-nums text-ink sm:table-cell">{nr(r.infrangeri)}</td>
                  <td className="py-1.5 text-center tabular-nums text-ink">
                    {nr(r.golMarcate)}:{nr(r.golPrimite)}
                  </td>
                  <td className="py-1.5 text-center tabular-nums text-ink">{dg != null && dg > 0 ? `+${dg}` : nr(dg)}</td>
                  <td className="py-1.5 text-center font-bold tabular-nums text-ink">{nr(r.puncte)}</td>
                  <td className="hidden py-1.5 md:table-cell">
                    <span className="flex justify-center gap-1">
                      {r.forma.map((m) => (
                        <ChipForma key={m.fixtureId} m={m} />
                      ))}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {zone.size > 0 && (
        <ul className="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-xs text-ink2">
          {[...zone].map(([nume, culoare]) => (
            <li key={nume} className="flex items-center gap-1.5">
              <span className={`h-3 w-3 rounded-sm ${culoare}`} />
              {nume}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
