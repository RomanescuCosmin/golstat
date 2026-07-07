import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { evenimentMeci } from '../../test/factories';
import { CronologieMeci } from './CronologieMeci';

describe('CronologieMeci', () => {
  test('minutul simplu si minutul cu prelungiri ("45+2\'")', () => {
    render(
      <CronologieMeci
        evenimente={[
          evenimentMeci({ id: 1, minut: 23 }),
          evenimentMeci({ id: 2, minut: 45, minutExtra: 2, jucator: 'Alt Jucator' }),
        ]}
      />,
    );
    expect(screen.getByText("23'")).toBeInTheDocument();
    expect(screen.getByText("45+2'")).toBeInTheDocument();
  });

  test('gol: numele marcatorului si pasa decisiva', () => {
    render(
      <CronologieMeci
        evenimente={[evenimentMeci({ tip: 'Goal', jucator: 'B. Fernandes', asist: 'M. Rashford' })]}
      />,
    );
    expect(screen.getByText('B. Fernandes')).toBeInTheDocument();
    expect(screen.getByText('pasă: M. Rashford')).toBeInTheDocument();
  });

  test('cartonas rosu vs galben: culori diferite pentru icon', () => {
    const { container } = render(
      <CronologieMeci
        evenimente={[
          evenimentMeci({ id: 1, tip: 'Card', detaliu: 'Red Card', jucator: 'X' }),
          evenimentMeci({ id: 2, tip: 'Card', detaliu: 'Yellow Card', jucator: 'Y' }),
        ]}
      />,
    );
    expect(container.querySelector('svg.text-accent')).not.toBeNull();
    expect(container.querySelector('svg[style*="rgb(227, 178, 59)"], svg[style*="#E3B23B"]')).not.toBeNull();
  });

  test('schimbare: cine intra si cine iese', () => {
    render(
      <CronologieMeci
        evenimente={[evenimentMeci({ tip: 'subst', jucator: 'A. Garnacho', detaliu: 'M. Rashford' })]}
      />,
    );
    expect(screen.getByText('A. Garnacho')).toBeInTheDocument();
    expect(screen.getByText('iese: M. Rashford')).toBeInTheDocument();
  });

  test('gazdele pe coloana stanga, oaspetii pe dreapta', () => {
    const { container } = render(
      <CronologieMeci
        evenimente={[
          evenimentMeci({ id: 1, gazde: true, jucator: 'JucatorGazda' }),
          evenimentMeci({ id: 2, gazde: false, jucator: 'JucatorOaspete' }),
        ]}
      />,
    );
    const randuri = container.querySelectorAll('li');
    const [randGazde, randOaspeti] = Array.from(randuri);
    expect(randGazde.children[0].textContent).toContain('JucatorGazda');
    expect(randGazde.children[2].textContent).toBe('');
    expect(randOaspeti.children[0].textContent).toBe('');
    expect(randOaspeti.children[2].textContent).toContain('JucatorOaspete');
  });
});
