import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { AntetPagina } from '../components/layout/AntetPagina';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/Card';
import { Comutator } from '../components/ui/Comutator';
import { RandSetare } from '../components/ui/RandSetare';
import { IconCeas, IconChevronRight, IconGear, IconGlobe, IconMoon, IconStar, IconSun, IconVolum } from '../components/ui/icons';
import { useFavorite } from '../hooks/useFavorite';
import { useMeciuriFavorite } from '../hooks/useMeciuriFavorite';
import { pregatesteSunet, seteazaSunet, sunetActiv, sunetGol } from '../lib/sunet';
import { useTheme, type Theme } from '../theme/ThemeProvider';

const TEME: { id: Theme; eticheta: string; icon: typeof IconMoon }[] = [
  { id: 'dark', eticheta: 'Întunecat', icon: IconMoon },
  { id: 'light', eticheta: 'Luminos', icon: IconSun },
];

function Grup({ titlu, children }: { titlu: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="mb-2 px-1 text-[11px] font-bold uppercase tracking-[0.12em] text-ink2">{titlu}</h2>
      <Card className="divide-y divide-line overflow-hidden">{children}</Card>
    </section>
  );
}

export function SetariPage() {
  const { theme, setTheme } = useTheme();
  const [sunet, setSunet] = useState(sunetActiv);
  const echipe = useFavorite().echipe.length;
  const meciuri = useMeciuriFavorite().meciuri.length;
  const fusOrar = Intl.DateTimeFormat().resolvedOptions().timeZone;

  function schimbaSunet(activ: boolean) {
    seteazaSunet(activ);
    setSunet(activ);
  }

  return (
    <PageLayout>
      <AntetPagina titlu="Setări" subtitlu="Aspectul aplicației și alertele de gol." icon={<IconGear width={22} height={22} />} />

      <div className="mx-auto max-w-3xl space-y-6">
        <Grup titlu="Aspect">
          <RandSetare icon={<IconMoon width={18} height={18} />} titlu="Temă" descriere="Întunecat este tema principală.">
            <div role="radiogroup" aria-label="Temă" className="flex rounded-full bg-bg p-1">
              {TEME.map(({ id, eticheta, icon: Icon }) => (
                <button
                  key={id}
                  type="button"
                  role="radio"
                  aria-checked={theme === id}
                  onClick={() => setTheme(id)}
                  className={`flex h-8 items-center gap-1.5 rounded-full px-3 text-xs font-semibold transition ${
                    theme === id ? 'bg-primary text-white shadow' : 'text-ink2 hover:text-ink'
                  }`}
                >
                  <Icon width={14} height={14} />
                  {eticheta}
                </button>
              ))}
            </div>
          </RandSetare>
        </Grup>

        <Grup titlu="Notificări">
          <RandSetare
            icon={<IconVolum width={18} height={18} />}
            titlu="Sunet la gol"
            descriere="Pentru meciurile favorite și meciurile echipelor favorite."
          >
            <Comutator activ={sunet} onSchimba={schimbaSunet} eticheta="Sunet la gol" />
          </RandSetare>
          <RandSetare icon={<IconVolum width={18} height={18} />} titlu="Testează sunetul">
            <button
              type="button"
              disabled={!sunet}
              onClick={() => {
                pregatesteSunet();
                sunetGol();
              }}
              className="h-9 rounded-btn border border-line px-4 text-xs font-semibold text-ink transition hover:bg-bg disabled:opacity-40"
            >
              Redă
            </button>
          </RandSetare>
        </Grup>

        <Grup titlu="Favorite">
          <Link to="/favorite" className="block transition hover:bg-bg">
            <RandSetare
              icon={<IconStar width={18} height={18} />}
              titlu="Echipe și meciuri urmărite"
              descriere={`${echipe} ${echipe === 1 ? 'echipă' : 'echipe'} · ${meciuri} ${meciuri === 1 ? 'meci' : 'meciuri'}`}
            >
              <IconChevronRight width={18} height={18} className="text-ink2" />
            </RandSetare>
          </Link>
        </Grup>

        <Grup titlu="Despre">
          <RandSetare icon={<IconCeas width={18} height={18} />} titlu="Fus orar" descriere="Orele meciurilor sunt afișate local.">
            <span className="text-xs font-semibold text-ink2">{fusOrar}</span>
          </RandSetare>
          <RandSetare
            icon={<IconGlobe width={18} height={18} />}
            titlu="Sursa datelor"
            descriere="Program, scoruri și statistici colectate automat; procentele sunt calculate de golstat."
          />
        </Grup>
      </div>
    </PageLayout>
  );
}
