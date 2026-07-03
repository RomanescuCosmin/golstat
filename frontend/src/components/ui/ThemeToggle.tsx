import { useTheme } from '../../theme/ThemeProvider';
import { IconMoon, IconSun } from './icons';

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const eDark = theme === 'dark';

  return (
    <button
      type="button"
      onClick={toggleTheme}
      title={eDark ? 'Comută pe mod luminos' : 'Comută pe mod întunecat'}
      aria-label={eDark ? 'Comută pe mod luminos' : 'Comută pe mod întunecat'}
      className="flex h-9 w-9 items-center justify-center rounded-full text-ink2 transition hover:bg-bg hover:text-ink dark:hover:bg-bg"
    >
      {eDark ? <IconSun /> : <IconMoon />}
    </button>
  );
}
