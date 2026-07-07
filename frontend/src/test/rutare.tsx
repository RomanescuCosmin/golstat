import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

/**
 * Randeaza o pagina care citeste parametri din ruta (`useParams`):
 * `randeazaCuRuta(<TeamPage />, { cale: '/echipa/:teamId', ruta: '/echipa/33' })`.
 */
export function randeazaCuRuta(ui: ReactElement, { cale, ruta }: { cale: string; ruta: string }) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <Routes>
        <Route path={cale} element={ui} />
      </Routes>
    </MemoryRouter>,
  );
}
