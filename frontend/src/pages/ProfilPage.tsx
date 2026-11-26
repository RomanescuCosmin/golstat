import { Link } from 'react-router-dom';
import { BENEFICII_PRO } from '../components/layout/CardPro';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { IconChart, IconCheck, IconChevronRight, IconClopot, IconDiamond, IconGear, IconStar, IconUser } from '../components/ui/icons';

const DETALII_PRO = [
  { icon: IconChart, titlu: 'Statistici avansate', text: 'Toate piețele, intervalele de goluri și forma pe locație.' },
  { icon: IconClopot, titlu: 'Alerte meciuri', text: 'Notificări la gol, pauză și final pentru meciurile urmărite.' },
  { icon: IconUser, titlu: 'Compară jucători', text: 'Statistici pe sezon, una lângă alta.' },
  { icon: IconDiamond, titlu: 'Fără reclame', text: 'Interfață curată, doar date.' },
];

const SCURTATURI = [
  { to: '/favorite', eticheta: 'Favorite', icon: IconStar },
  { to: '/setari', eticheta: 'Setări', icon: IconGear },
];

/** Profilul utilizatorului si oferta golstat PRO. Datele contului vin din autentificare (in lucru). */
export function ProfilPage() {
  return (
    <PageLayout>
      <h1 className="sr-only lg:not-sr-only lg:mb-5 lg:text-2xl lg:font-extrabold lg:text-ink">Profil</h1>

      <div className="mx-auto grid max-w-5xl items-start gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <div className="space-y-4">
          <Card className="flex flex-col items-center px-5 py-6 text-center">
            <span className="flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-primary to-[#7C3AED] text-3xl font-extrabold text-white ring-4 ring-primary/20">
              A
            </span>
            <p className="mt-3 text-lg font-extrabold text-ink">Alex Pop</p>
            <span className="mt-1 rounded-md bg-accent px-2 py-0.5 text-[11px] font-extrabold text-white">PRO</span>
          </Card>

          <Card className="divide-y divide-line overflow-hidden">
            {SCURTATURI.map(({ to, eticheta, icon: Icon }) => (
              <Link key={to} to={to} className="flex min-h-[56px] items-center gap-3 px-4 transition hover:bg-bg">
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15 text-primary">
                  <Icon width={18} height={18} />
                </span>
                <span className="flex-1 text-sm font-semibold text-ink">{eticheta}</span>
                <IconChevronRight width={18} height={18} className="text-ink2" />
              </Link>
            ))}
          </Card>
        </div>

        <Card className="relative overflow-hidden p-5 sm:p-6">
          <span aria-hidden className="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-primary/20 blur-3xl" />
          <p className="flex items-center gap-2 text-base font-extrabold">
            <IconDiamond width={22} height={22} className="text-primary" />
            <span className="text-primary">golstat</span>
            <span className="rounded-md bg-accent px-1.5 py-0.5 text-[11px] font-extrabold text-white">PRO</span>
          </p>
          <p className="mt-1 text-sm text-ink2">Tot ce ai nevoie ca să citești meciul înainte de fluierul de start.</p>

          <ul className="mt-5 grid gap-3 sm:grid-cols-2">
            {DETALII_PRO.map(({ icon: Icon, titlu, text }) => (
              <li key={titlu} className="rounded-2xl border border-line bg-bg/50 p-4">
                <Icon width={20} height={20} className="text-primary" />
                <p className="mt-2 text-sm font-semibold text-ink">{titlu}</p>
                <p className="mt-0.5 text-xs leading-snug text-ink2">{text}</p>
              </li>
            ))}
          </ul>

          <ul className="mt-5 flex flex-wrap gap-x-4 gap-y-2">
            {BENEFICII_PRO.map((b) => (
              <li key={b} className="flex items-center gap-1.5 text-xs text-ink2">
                <IconCheck width={14} height={14} className="text-primary" />
                {b}
              </li>
            ))}
          </ul>

          <button
            type="button"
            disabled
            title="În curând"
            className="mt-6 flex h-12 w-full items-center justify-center gap-2 rounded-btn bg-gradient-to-r from-primary to-[#7C3AED] text-sm font-bold text-white opacity-80"
          >
            Upgrade — în curând
          </button>
        </Card>
      </div>
    </PageLayout>
  );
}
