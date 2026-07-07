import { describe, expect, test, vi } from 'vitest';

// Fus negativ fata de UTC, ca sa prinda regresia: "2026-07-07" parsat ca miez de noapte UTC
// ar deveni "6 iulie" la New York. TZ trebuie setat INAINTE ca modulul format sa isi construiasca
// formatterele Intl (sunt la nivel de modul), de aceea importul e dinamic, in test.
vi.stubEnv('TZ', 'America/New_York');

describe('formatData intr-un fus negativ (TZ=America/New_York)', () => {
  test('LocalDate "YYYY-MM-DD" nu sare cu o zi inapoi', async () => {
    const { formatData, formatDataScurta } = await import('./format');
    expect(formatData('2026-07-07')).toBe('7 iulie 2026');
    expect(formatDataScurta('2026-07-07')).toBe('07.07.2026');
  });
});
