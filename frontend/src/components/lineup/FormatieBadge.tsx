interface FormatieBadgeProps {
  nume: string | null;
  formatie: string | null;
  /** Alinierea in coltul terenului. */
  aliniere?: 'stanga' | 'dreapta';
}

/** Eticheta din coltul terenului: numele echipei + formatia (ex. "Real Madrid 4-3-1-2"). */
export function FormatieBadge({ nume, formatie, aliniere = 'stanga' }: FormatieBadgeProps) {
  return (
    <div className={aliniere === 'dreapta' ? 'text-right' : 'text-left'}>
      <span className="text-sm font-bold text-ink">{nume ?? 'Echipă'}</span>
      {formatie && <span className="ml-2 text-xs font-semibold text-ink2">{formatie}</span>}
    </div>
  );
}
