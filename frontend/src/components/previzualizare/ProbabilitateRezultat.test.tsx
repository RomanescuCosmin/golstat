import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { predictieMeci, procentCota } from '../../test/factories';
import { ProbabilitateRezultat } from './ProbabilitateRezultat';

function zone(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>('[style]'));
}

describe('ProbabilitateRezultat', () => {
  test('afiseaza cele trei zone cu procentele si latimi proportionale', () => {
    const { container } = render(
      <ProbabilitateRezultat
        predictie={predictieMeci({ gazde: procentCota(50), egal: procentCota(30), oaspeti: procentCota(20) })}
      />,
    );
    expect(screen.getByText('50%')).toBeInTheDocument();
    expect(screen.getByText('30%')).toBeInTheDocument();
    expect(screen.getByText('20%')).toBeInTheDocument();
    const [z1, z2, z3] = zone(container);
    expect(z1.style.flexGrow).toBe('50');
    expect(z2.style.flexGrow).toBe('30');
    // sub pragul minim de 18 zona nu se mai ingusteaza (eticheta ramane lizibila)
    expect(z3.style.flexGrow).toBe('20');
  });

  test('procent NaN: latimea cade pe pragul minim, textul pe "—", fara NaN in layout', () => {
    const { container } = render(
      <ProbabilitateRezultat
        predictie={predictieMeci({ gazde: { procent: NaN, cota: NaN }, egal: procentCota(30), oaspeti: procentCota(20) })}
      />,
    );
    expect(zone(container)[0].style.flexGrow).toBe('18');
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(container.textContent).not.toContain('NaN');
  });

  test('meci viitor (fara rezultat): nu apare badge-ul Corect/Gresit', () => {
    render(<ProbabilitateRezultat predictie={predictieMeci({ rezultat: null })} />);
    expect(screen.queryByText(/Corect|Greșit/)).toBeNull();
  });

  test('pick-ul modelului corect (gazde favorite, gazdele castiga): badge "✓ Corect" pe zona reala', () => {
    render(
      <ProbabilitateRezultat
        predictie={predictieMeci({
          gazde: procentCota(50),
          egal: procentCota(30),
          oaspeti: procentCota(20),
          rezultat: { goluriGazde: 2, goluriOaspeti: 0, statusShort: 'FT' },
        })}
      />,
    );
    expect(screen.getByText('✓ Corect')).toBeInTheDocument();
    expect(screen.getByText('rezultat')).toBeInTheDocument();
  });

  test('pick gresit (gazde favorite, castiga oaspetii): badge "✗ Greșit"', () => {
    render(
      <ProbabilitateRezultat
        predictie={predictieMeci({
          gazde: procentCota(50),
          egal: procentCota(30),
          oaspeti: procentCota(20),
          rezultat: { goluriGazde: 0, goluriOaspeti: 2, statusShort: 'FT' },
        })}
      />,
    );
    expect(screen.getByText('✗ Greșit')).toBeInTheDocument();
  });

  test('egal la final: zona "Egal" e marcata drept rezultat', () => {
    render(
      <ProbabilitateRezultat
        predictie={predictieMeci({
          gazde: procentCota(50),
          egal: procentCota(30),
          oaspeti: procentCota(20),
          rezultat: { goluriGazde: 1, goluriOaspeti: 1, statusShort: 'FT' },
        })}
      />,
    );
    // pick = gazde (50) dar rezultat = egal → gresit
    expect(screen.getByText('✗ Greșit')).toBeInTheDocument();
    const zonaEgal = screen.getByText('Egal').closest('[style]');
    expect(zonaEgal?.textContent).toContain('rezultat');
  });
});
