import { useEffect, useState } from 'react';
import { ApiError, getPrevizualizare } from '../../api/client';
import type { PrevizualizareMeciDto } from '../../api/types';
import { SectiuneStatistici } from '../previzualizare/SectiuneStatistici';
import { Card } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { Spinner } from '../ui/Spinner';

/**
 * Verdictul pe piețe al unui meci TERMINAT: fiecare linie primește ✓ verde sau ✗ roșu, după cum
 * partea favorizată de model (probabilitate ≥ 50%) s-a potrivit sau nu cu ce s-a întâmplat.
 *
 * Refolosește `SectiuneStatistici` din previzualizare în loc să dubleze logica: acolo badge-urile
 * de rezultat există deja pentru toate piețele și — important — deosebesc „fără date" (furnizorul
 * n-are statistica) de „ratat", ceea ce altfel s-ar citi greșit ca verdict negativ.
 */
export function VerdictePiete({ fixtureId }: { fixtureId: number }) {
  const [date, setDate] = useState<PrevizualizareMeciDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [eroare, setEroare] = useState<ApiError | null>(null);

  useEffect(() => {
    let anulat = false;
    setLoading(true);
    setEroare(null);
    getPrevizualizare(fixtureId)
      .then((rezultat) => {
        if (!anulat) setDate(rezultat);
      })
      .catch((e: unknown) => {
        if (!anulat) {
          setDate(null);
          setEroare(e instanceof ApiError ? e : new ApiError(0, 'Eroare neașteptată', String(e)));
        }
      })
      .finally(() => {
        if (!anulat) setLoading(false);
      });
    return () => {
      anulat = true;
    };
  }, [fixtureId]);

  if (loading) {
    return (
      <Card className="flex justify-center py-10">
        <Spinner size={28} />
      </Card>
    );
  }

  // Predicția cere istoric pe ambele echipe; la un meci de cupă cu echipe noi poate lipsi. Nu e o
  // eroare de afișat cu roșu — doar n-avem ce evalua.
  if (eroare || !date) {
    return (
      <Card>
        <EmptyState
          titlu="Verdict indisponibil"
          mesaj="Nu există suficient istoric pentru a evalua piețele acestui meci."
        />
      </Card>
    );
  }

  return (
    <SectiuneStatistici
      statistici={date.statistici}
      gazde={date.predictie.echipaGazde}
      oaspeti={date.predictie.echipaOaspeti}
    />
  );
}
