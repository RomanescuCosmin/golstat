import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test, vi } from 'vitest';
import type { NotificareGol } from '../../hooks/useNotificariGol';
import { CardNotificare } from './NotificareGol';

function notificare(over: Partial<NotificareGol> = {}): NotificareGol {
  return {
    cheie: '1:1-0:h',
    fixtureId: 1,
    gazde: { id: 10, nume: 'Steaua', logo: null },
    oaspeti: { id: 20, nume: 'Dinamo', logo: null },
    golGazde: 1,
    golOaspeti: 0,
    minut: 23,
    marcatorAcasa: true,
    ligaNume: 'Liga I',
    ...over,
  };
}

function randeaza(n: NotificareGol, onClose = () => {}) {
  return render(
    <MemoryRouter>
      <CardNotificare n={n} onClose={onClose} />
    </MemoryRouter>,
  );
}

describe('CardNotificare', () => {
  test('afiseaza ambele echipe, scorul si liga', () => {
    randeaza(notificare());
    expect(screen.getByText('Steaua')).toBeInTheDocument();
    expect(screen.getByText('Dinamo')).toBeInTheDocument();
    expect(screen.getByText('Liga I')).toBeInTheDocument();
    // scor 1-0 vizibil, fara valori goale / NaN
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
    expect(document.body.textContent).not.toMatch(/NaN|undefined|null/);
  });

  test('eticheta GOOL apare pe randul marcatorului (gazde) cand acasa inscrie', () => {
    randeaza(notificare({ marcatorAcasa: true }));
    // linkul care duce la Match Center are randurile; verificam ca "Gool" apare o singura data (marcator)
    expect(screen.getAllByText(/⚽ Gool/i)).toHaveLength(1);
    // antetul afiseaza GOOL + minutul
    expect(screen.getByText(/GOOL · 23'/)).toBeInTheDocument();
  });

  test('cand inscriu oaspetii, marcatorul e celalalt rand', () => {
    randeaza(notificare({ marcatorAcasa: false, golGazde: 0, golOaspeti: 1, cheie: '1:0-1:a' }));
    expect(screen.getAllByText(/⚽ Gool/i)).toHaveLength(1);
    expect(screen.getByText('Dinamo')).toBeInTheDocument();
  });

  test('butonul de inchidere apeleaza onClose', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    randeaza(notificare(), onClose);
    await user.click(screen.getByRole('button', { name: 'Închide' }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
