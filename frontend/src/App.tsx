import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeProvider';
import { AppShell } from './components/layout/AppShell';
import { MeciuriPage } from './pages/MeciuriPage';
import { PrevizualizarePage } from './pages/PrevizualizarePage';
import { LivePage } from './pages/LivePage';

export function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/" element={<MeciuriPage />} />
            <Route path="/meci/:fixtureId" element={<PrevizualizarePage />} />
            <Route path="/live" element={<LivePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}
