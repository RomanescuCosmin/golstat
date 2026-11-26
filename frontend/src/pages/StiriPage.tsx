import { Link } from 'react-router-dom';
import { AntetPagina } from '../components/layout/AntetPagina';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { IconCalendar, IconChevronRight, IconLive, IconStiri } from '../components/ui/icons';

const SCURTATURI = [
  { to: '/live', eticheta: 'Meciuri live', descriere: 'Scoruri în timp real', icon: IconLive },
  { to: '/program', eticheta: 'Program', descriere: 'Următoarele 7 zile', icon: IconCalendar },
];

/** Sectiunea de stiri: inca fara sursa de date; pastreaza locul in navigatie si trimite spre ce exista. */
export function StiriPage() {
  return (
    <PageLayout>
      <AntetPagina titlu="Știri" subtitlu="Noutăți din competițiile urmărite." icon={<IconStiri width={22} height={22} />} />

      <div className="mx-auto max-w-3xl space-y-4">
        <Card>
          <EmptyState
            titlu="Știrile vin în curând"
            mesaj="Lucrăm la fluxul de știri. Până atunci, urmărește meciurile live sau programul."
          />
        </Card>

        <div className="grid gap-3 sm:grid-cols-2">
          {SCURTATURI.map(({ to, eticheta, descriere, icon: Icon }) => (
            <Link key={to} to={to} className="group">
              <Card className="flex items-center gap-3 p-4 transition group-hover:border-primary/40">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/15 text-primary">
                  <Icon width={20} height={20} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-semibold text-ink">{eticheta}</span>
                  <span className="block text-xs text-ink2">{descriere}</span>
                </span>
                <IconChevronRight width={18} height={18} className="text-ink2" />
              </Card>
            </Link>
          ))}
        </div>
      </div>
    </PageLayout>
  );
}
