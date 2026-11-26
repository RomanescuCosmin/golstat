import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { citesteSiCurataLinkEmail } from './lib/linkEmail';
import './styles/index.css';

// inainte de render: StrictMode ruleaza efectele de doua ori, iar tokenul trebuie scos din URL imediat
const linkEmail = citesteSiCurataLinkEmail();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App linkEmail={linkEmail} />
  </StrictMode>,
);
