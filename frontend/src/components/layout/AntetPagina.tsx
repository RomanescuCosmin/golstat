import type { ReactNode } from 'react';

interface AntetPaginaProps {
  titlu: ReactNode;
  subtitlu?: ReactNode;
  icon?: ReactNode;
  /** Controalele paginii (selector liga, filtre); pe mobil coboara sub titlu, pe toata latimea. */
  actiuni?: ReactNode;
  /** Element langa titlu (ex. numarul de meciuri live). */
  insigna?: ReactNode;
}

/**
 * Antetul comun al paginilor de sectiune. Pe mobil titlul e deja in header-ul aplicatiei, asa ca aici
 * ramane doar ca titlu accesibil, iar subtitlul si actiunile urca in capul paginii.
 */
export function AntetPagina({ titlu, subtitlu, icon, actiuni, insigna }: AntetPaginaProps) {
  return (
    <div className="mb-4 flex flex-col gap-3 md:mb-5 md:flex-row md:flex-wrap md:items-center md:gap-4">
      <div className="flex min-w-0 flex-1 items-start gap-3">
        {icon && (
          <span className="mt-0.5 hidden h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary/15 text-primary lg:flex">
            {icon}
          </span>
        )}
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h1 className="sr-only text-2xl font-extrabold text-ink lg:not-sr-only">{titlu}</h1>
            {insigna}
          </div>
          {subtitlu && <p className="text-[13px] leading-snug text-ink2 md:text-sm">{subtitlu}</p>}
        </div>
      </div>
      {actiuni && <div className="flex min-w-0 flex-wrap items-center gap-2 [&>*]:max-w-full">{actiuni}</div>}
    </div>
  );
}
