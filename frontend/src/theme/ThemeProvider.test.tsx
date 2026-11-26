import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { ThemeProvider, useTheme } from './ThemeProvider';

function Proba() {
  const { theme, toggleTheme } = useTheme();
  return (
    <button type="button" onClick={toggleTheme}>
      tema: {theme}
    </button>
  );
}

afterEach(() => {
  document.documentElement.classList.remove('dark');
});

describe('ThemeProvider', () => {
  test('implicit dark (tema principala a aplicatiei)', () => {
    render(
      <ThemeProvider>
        <Proba />
      </ThemeProvider>,
    );
    expect(screen.getByText('tema: dark')).toBeInTheDocument();
    expect(document.documentElement).toHaveClass('dark');
  });

  test('toggle: comuta pe light, scoate clasa de pe <html> si persista in localStorage', async () => {
    const user = userEvent.setup();
    render(
      <ThemeProvider>
        <Proba />
      </ThemeProvider>,
    );
    await user.click(screen.getByRole('button'));

    expect(screen.getByText('tema: light')).toBeInTheDocument();
    expect(document.documentElement).not.toHaveClass('dark');
    expect(localStorage.getItem('golstat-theme')).toBe('light');
  });

  test('tema salvata anterior are prioritate la pornire', () => {
    localStorage.setItem('golstat-theme', 'light');
    render(
      <ThemeProvider>
        <Proba />
      </ThemeProvider>,
    );
    expect(screen.getByText('tema: light')).toBeInTheDocument();
  });

  test('useTheme in afara provider-ului: eroare explicita', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Proba />)).toThrow('useTheme trebuie folosit în interiorul unui ThemeProvider');
    consoleError.mockRestore();
  });
});
