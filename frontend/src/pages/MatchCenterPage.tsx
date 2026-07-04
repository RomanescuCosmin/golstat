import { Link, useParams } from 'react-router-dom';
import { useMatchCenter } from '../hooks/useMatchCenter';
import { CronologieMeci } from '../components/centru/CronologieMeci';
import { HeaderScor } from '../components/centru/HeaderScor';
import { LiveAcumRail } from '../components/centru/LiveAcumRail';
import { StatisticiLive } from '../components/centru/StatisticiLive';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';

export function MatchCenterPage() {
  const { fixtureId } = useParams<{ fixtureId: string }>();
  const { date, loading, eroare, reincarca, live } = useMatchCenter(fixtureId);

  return (
    <PageLayout rightRail={<LiveAcumRail />}>
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink2" aria-label="Breadcrumb">
        <Link to="/" className="font-medium hover:text-primary">
          Meciuri
        </Link>
        <span aria-hidden>›</span>
        <span className="font-semibold text-ink">Centru meci</span>
      </nav>

      {loading && (
        <div className="flex justify-center py-20">
          <Spinner size={36} />
        </div>
      )}

      {!loading && eroare && (
        <Card>
          {eroare.status === 404 ? (
            <ErrorState titlu="Meciul nu a fost găsit" mesaj={eroare.detail ?? undefined} />
          ) : (
            <ErrorState titlu={eroare.title} mesaj={eroare.detail ?? eroare.message} onRetry={reincarca} />
          )}
        </Card>
      )}

      {!loading && !eroare && date && (
        <div className="space-y-5">
          <HeaderScor meci={date} live={live} />

          {date.statistici ? (
            <StatisticiLive gazde={date.gazde} oaspeti={date.oaspeti} statistici={date.statistici} />
          ) : (
            <Card>
              <EmptyState titlu="Statistici indisponibile" mesaj="Statistici indisponibile pentru acest meci." />
            </Card>
          )}

          {date.evenimente.length > 0 ? (
            <CronologieMeci evenimente={date.evenimente} />
          ) : (
            <Card>
              <EmptyState titlu="Fără evenimente" mesaj="Fără evenimente înregistrate." />
            </Card>
          )}
        </div>
      )}
    </PageLayout>
  );
}
