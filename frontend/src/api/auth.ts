import { cerere } from './client';

/** Utilizatorul autentificat (`GET /v1/auth/eu`). */
export interface Utilizator {
  id: number;
  email: string;
  nume: string;
}

const AUTH = '/v1/auth';

/** Utilizatorul sesiunii curente sau `null` pentru vizitatori (serverul raspunde 204). */
export async function getEu(): Promise<Utilizator | null> {
  return (await cerere<Utilizator | undefined>(`${AUTH}/eu`)) ?? null;
}

export function autentificare(email: string, parola: string): Promise<Utilizator> {
  return cerere<Utilizator>(`${AUTH}/autentificare`, { method: 'POST', body: { email, parola } });
}

export function deconectare(): Promise<void> {
  return cerere<void>(`${AUTH}/deconectare`, { method: 'POST' });
}

/** Raspunsul e identic indiferent daca emailul are deja cont (anti-enumerare). */
export function inregistrare(email: string, parola: string, nume: string): Promise<void> {
  return cerere<void>(`${AUTH}/inregistrare`, { method: 'POST', body: { email, parola, nume } });
}

/** Confirmarea cere si parola contului (protectie impotriva conturilor pre-inregistrate de altcineva). */
export function confirmareEmail(token: string, parola: string): Promise<void> {
  return cerere<void>(`${AUTH}/confirmare-email`, { method: 'POST', body: { token, parola } });
}

export function retrimiteConfirmare(email: string): Promise<void> {
  return cerere<void>(`${AUTH}/retrimite-confirmare`, { method: 'POST', body: { email } });
}

export function parolaUitata(email: string): Promise<void> {
  return cerere<void>(`${AUTH}/parola-uitata`, { method: 'POST', body: { email } });
}

export function resetareParola(token: string, parolaNoua: string): Promise<void> {
  return cerere<void>(`${AUTH}/resetare-parola`, { method: 'POST', body: { token, parolaNoua } });
}

export function schimbaParola(parolaCurenta: string, parolaNoua: string): Promise<void> {
  return cerere<void>(`${AUTH}/schimba-parola`, { method: 'POST', body: { parolaCurenta, parolaNoua } });
}
