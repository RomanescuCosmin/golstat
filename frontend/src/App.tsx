import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeProvider';
import { AppShell } from './components/layout/AppShell';
import { MeciuriPage } from './pages/MeciuriPage';
import { PrevizualizarePage } from './pages/PrevizualizarePage';
import { MatchCenterPage } from './pages/MatchCenterPage';
import { TeamPage } from './pages/TeamPage';
import { LivePage } from './pages/LivePage';
import { ProgramPage } from './pages/ProgramPage';
import { CompetitiePage } from './pages/CompetitiePage';
import { StatisticiPage } from './pages/StatisticiPage';
import { PietePage } from './pages/PietePage';
import { JucatorPage } from './pages/JucatorPage';
import { EchipePage } from './pages/EchipePage';
import { JucatoriPage } from './pages/JucatoriPage';

export function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/" element={<MeciuriPage />} />
            <Route path="/meci/:fixtureId" element={<PrevizualizarePage />} />
            <Route path="/meci/:fixtureId/centru" element={<MatchCenterPage />} />
            <Route path="/echipa/:teamId" element={<TeamPage />} />
            <Route path="/competitie/:leagueId" element={<CompetitiePage />} />
            <Route path="/statistici" element={<StatisticiPage />} />
            <Route path="/piete" element={<PietePage />} />
            <Route path="/echipe" element={<EchipePage />} />
            <Route path="/jucatori" element={<JucatoriPage />} />
            <Route path="/jucator/:playerId" element={<JucatorPage />} />
            <Route path="/program" element={<ProgramPage />} />
            <Route path="/live" element={<LivePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}
