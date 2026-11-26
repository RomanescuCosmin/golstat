import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, test } from 'vitest';
import { ThemeProvider } from '../theme/ThemeProvider';
import { sunetActiv } from '../lib/sunet';
import { SetariPage } from './SetariPage';

function randeaza() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <SetariPage />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

afterEach(() => {
  localStorage.clear();
  document.documentElement.classList.remove('dark');
});

describe('SetariPage', () => {
  test('tema se alege explicit si se aplica pe <html>', async () => {
    const user = userEvent.setup();
    randeaza();
    expect(screen.getByRole('radio', { name: 'Întunecat' })).toHaveAttribute('aria-checked', 'true');

    await user.click(screen.getByRole('radio', { name: 'Luminos' }));
    expect(document.documentElement).not.toHaveClass('dark');
    expect(localStorage.getItem('golstat-theme')).toBe('light');
  });

  test('sunetul la gol se poate opri si preferinta persista', async () => {
    const user = userEvent.setup();
    randeaza();
    const comutator = screen.getByRole('switch', { name: 'Sunet la gol' });
    expect(comutator).toHaveAttribute('aria-checked', 'true');

    await user.click(comutator);
    expect(comutator).toHaveAttribute('aria-checked', 'false');
    expect(sunetActiv()).toBe(false);
    expect(screen.getByRole('button', { name: 'Redă' })).toBeDisabled();
  });
});
