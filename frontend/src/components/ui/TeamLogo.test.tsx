import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { LigaLogo } from './LigaLogo';
import { TeamLogo } from './TeamLogo';

describe('TeamLogo', () => {
  test('cu logo: imaginea cu alt-ul echipei', () => {
    render(<TeamLogo nume="Manchester United" logo="https://exemplu/logo.png" />);
    expect(screen.getByAltText('Manchester United')).toHaveAttribute('src', 'https://exemplu/logo.png');
  });

  test('fara logo: initialele din primele doua cuvinte', () => {
    render(<TeamLogo nume="FC Botoșani" logo={null} />);
    expect(screen.getByText('FB')).toBeInTheDocument();
  });

  test('fara nume si fara logo: "?"', () => {
    render(<TeamLogo nume={null} logo={null} />);
    expect(screen.getByText('?')).toBeInTheDocument();
  });

  test('imaginea pica (onError): fallback la initiale', () => {
    render(<TeamLogo nume="Manchester United" logo="https://exemplu/rupt.png" />);
    fireEvent.error(screen.getByAltText('Manchester United'));
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.getByText('MU')).toBeInTheDocument();
  });
});

describe('LigaLogo (lantul de fallback)', () => {
  test('liga obisnuita cu id: imagine de pe CDN', () => {
    render(<LigaLogo id={39} nume="Premier League" />);
    expect(screen.getByAltText('Premier League')).toHaveAttribute(
      'src',
      'https://media.api-sports.io/football/leagues/39.png',
    );
  });

  test('Campionatul Mondial (id 1, sursa are doar placeholder): logo propriu, nu imagine CDN', () => {
    const { container } = render(<LigaLogo id={1} nume="Campionatul Mondial 2026" />);
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('svg')).not.toBeNull();
  });

  test('fara id si fara logo: trofeul generic', () => {
    const { container } = render(<LigaLogo nume="Competiție" />);
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('svg')).not.toBeNull();
  });

  test('imaginea pica: fallback la trofeu', () => {
    const { container } = render(<LigaLogo id={39} nume="Premier League" />);
    fireEvent.error(screen.getByAltText('Premier League'));
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('svg')).not.toBeNull();
  });
});
