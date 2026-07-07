import { describe, expect, test } from 'vitest';
import {
  formatCota,
  formatData,
  formatDataScurta,
  formatOra,
  formatProcent,
  formatRata,
  toISODataLocala,
} from './format';

describe('formatData / formatDataScurta', () => {
  test('LocalDate "YYYY-MM-DD" se afiseaza ca zi calendaristica, nu ca moment UTC', () => {
    expect(formatData('2026-07-07')).toBe('7 iulie 2026');
    expect(formatDataScurta('2026-07-07')).toBe('07.07.2026');
  });

  test('OffsetDateTime complet functioneaza in continuare', () => {
    expect(formatData('2026-07-03T22:00:00+03:00')).toMatch(/iulie 2026/);
    expect(formatDataScurta('2026-12-01T10:00:00+02:00')).toBe('01.12.2026');
  });
});

describe('formatOra', () => {
  test('extrage ora si minutul dintr-un ISO cu ora', () => {
    // ora exacta depinde de fusul masinii; verificam formatul HH:MM
    expect(formatOra('2026-07-03T22:00:00+03:00')).toMatch(/^\d{2}:\d{2}$/);
  });
});

describe('formatProcent', () => {
  test('rotunjeste la intreg si adauga sufixul %', () => {
    expect(formatProcent(0)).toBe('0%');
    expect(formatProcent(62.4)).toBe('62%');
    expect(formatProcent(62.5)).toBe('63%');
    expect(formatProcent(100)).toBe('100%');
  });

  test('valori nevalide: "—", niciodata "NaN%"', () => {
    expect(formatProcent(NaN)).toBe('—');
    expect(formatProcent(Infinity)).toBe('—');
    expect(formatProcent(-Infinity)).toBe('—');
  });
});

describe('formatRata', () => {
  test('fractia 0..1 devine procent', () => {
    expect(formatRata(0.62)).toBe('62%');
    expect(formatRata(1)).toBe('100%');
    expect(formatRata(NaN)).toBe('—');
  });
});

describe('formatCota', () => {
  test('doua zecimale', () => {
    expect(formatCota(2.1)).toBe('2.10');
    expect(formatCota(100 / 45)).toBe('2.22');
  });
});

describe('toISODataLocala', () => {
  test('componentele locale, cu zero-padding', () => {
    expect(toISODataLocala(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(toISODataLocala(new Date(2026, 11, 31))).toBe('2026-12-31');
  });

  test('roundtrip cu formatData: aceeasi zi calendaristica', () => {
    const azi = new Date(2026, 6, 7, 23, 59);
    expect(formatData(toISODataLocala(azi))).toBe('7 iulie 2026');
  });
});
