import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPredictiiZi } from '../api/client';
import type { PredictieMeciDto } from '../api/types';
import { MatchCard } from '../components/meciuri/MatchCard';
import { SelectorLiga } from '../components/meciuri/SelectorLiga';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { Spinner } from '../components/ui/Spinner';
import { esteInPlay, useLiveScores } from '../hooks/useLiveScore';
import { toISODataLocala } from '../lib/format';
import { LIGI, numeLiga } from '../lib/ligi';

/**
 * Pagina Live: se aboneaza la meciurile zilei (sursa de id-uri: predictiile ligii selectate —
 * nu exista endpoint de descoperire live) si afiseaza doar cardurile in desfasurare.
 * Daca WebSocket-ul nu e disponibil, pur si simplu nu apar update-uri; nimic nu crapa.
 */
export function LivePage() {
  const navigate = useNavigate();
  const [leagueId, setLeagueId] = useState(LIGI[0]!.id);
  const [meciuri, setMeciuri] = useState<PredictieMeciDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    getPredictiiZi(leagueId, toISODataLocala(new Date()))
      .then((rezultat) => {
        if (!anulat) setMeciuri(rezultat);
      })
      .catch(() => {
        // Backend indisponibil sau zi fara predictii: ramanem pe empty state, fara eroare.
        if (!anulat) setMeciuri([]);
      })
      .finally(() => {
        if (!anulat) setLoading(false);
      });
    return () => {
      anulat = true;
    };
  }, [leagueId]);

  const scoruri = useLiveScores(meciuri.map((m) => m.fixtureId));
  const inDesfasurare = meciuri.filter((m) => esteInPlay(scoruri[m.fixtureId]));

  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <h1 className="text-lg font-bold">Live</h1>
        <div className="ml-auto">
          <SelectorLiga leagueId={leagueId} onChange={setLeagueId} />
        </div>
      </div>

      {loading && (
        <div className="flex justify-center py-20">
          <Spinner size={36} />
        </div>
      )}

      {!loading && inDesfasurare.length === 0 && (
        <Card>
          <EmptyState
            titlu="Niciun meci în desfășurare acum"
            mesaj="Când un meci din ziua curentă intră în desfășurare, scorul lui apare aici în timp real."
          />
        </Card>
      )}

      {!loading && inDesfasurare.length > 0 && (
        <div className="space-y-4">
          {inDesfasurare.map((meci) => (
            <MatchCard
              key={meci.fixtureId}
              meci={meci}
              numeLiga={numeLiga(leagueId)}
              onOpen={() => navigate(`/meci/${meci.fixtureId}`)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
