const dataLunga = new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' });
const dataScurta = new Intl.DateTimeFormat('ro-RO', { day: '2-digit', month: '2-digit', year: 'numeric' });
const oraMinut = new Intl.DateTimeFormat('ro-RO', { hour: '2-digit', minute: '2-digit' });

/** "3 iulie 2026" dintr-un ISO (OffsetDateTime sau LocalDate). */
export function formatData(iso: string): string {
  return dataLunga.format(new Date(iso));
}

/** "03.07.2026" dintr-un ISO. */
export function formatDataScurta(iso: string): string {
  return dataScurta.format(new Date(iso));
}

/** "22:00" dintr-un ISO cu ora. */
export function formatOra(iso: string): string {
  return oraMinut.format(new Date(iso));
}

/** Procent 0..100 → "62%"; "—" pentru valori nevalide (nu afisam niciodata "NaN%"). */
export function formatProcent(procent: number): string {
  return Number.isFinite(procent) ? `${Math.round(procent)}%` : '—';
}

/** Fractie 0..1 (OverUnder) → "62%". */
export function formatRata(rata: number): string {
  return formatProcent(rata * 100);
}

/** Cota statistica → "2.10". */
export function formatCota(cota: number): string {
  return cota.toFixed(2);
}

/** Data locala → "YYYY-MM-DD" (fara conversie UTC, ca sa nu sara ziua). */
export function toISODataLocala(d: Date): string {
  const luna = String(d.getMonth() + 1).padStart(2, '0');
  const zi = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${luna}-${zi}`;
}
