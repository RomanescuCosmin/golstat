import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react';
import { ApiError } from '../../api/client';

const CLASE_CAMP =
  'h-11 w-full rounded-input border border-line bg-bg px-3 text-sm text-ink placeholder:text-ink2/60 transition focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30 aria-[invalid=true]:border-red-500';

interface CampProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  eticheta: string;
  eroare?: string | null;
  ajutor?: string;
}

export function Camp({ eticheta, eroare, ajutor, ...rest }: CampProps) {
  const id = useId();
  const idDetaliu = `${id}-detaliu`;
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium text-ink">
        {eticheta}
      </label>
      <input
        id={id}
        className={CLASE_CAMP}
        aria-invalid={eroare ? true : undefined}
        aria-describedby={eroare || ajutor ? idDetaliu : undefined}
        {...rest}
      />
      {(eroare || ajutor) && (
        <p id={idDetaliu} className={`text-xs ${eroare ? 'text-red-500' : 'text-ink2'}`}>
          {eroare ?? ajutor}
        </p>
      )}
    </div>
  );
}

/** Camp de parola cu buton Arată/Ascunde; `autoComplete` spune managerului de parole ce e campul. */
export function CampParola({
  eticheta,
  eroare,
  ajutor,
  autoComplete,
  ...rest
}: CampProps & { autoComplete: 'current-password' | 'new-password' }) {
  const id = useId();
  const idDetaliu = `${id}-detaliu`;
  const [vizibila, setVizibila] = useState(false);
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium text-ink">
        {eticheta}
      </label>
      <div className="relative">
        <input
          id={id}
          type={vizibila ? 'text' : 'password'}
          autoComplete={autoComplete}
          maxLength={128}
          className={`${CLASE_CAMP} pr-20`}
          aria-invalid={eroare ? true : undefined}
          aria-describedby={eroare || ajutor ? idDetaliu : undefined}
          {...rest}
        />
        <button
          type="button"
          onClick={() => setVizibila((v) => !v)}
          aria-pressed={vizibila}
          aria-label="Arată parola"
          aria-controls={id}
          className="absolute inset-y-0 right-2 my-auto h-8 rounded-lg px-2 text-xs font-semibold text-ink2 hover:text-ink"
        >
          {vizibila ? 'Ascunde' : 'Arată'}
        </button>
      </div>
      {(eroare || ajutor) && (
        <p id={idDetaliu} className={`text-xs ${eroare ? 'text-red-500' : 'text-ink2'}`}>
          {eroare ?? ajutor}
        </p>
      )}
    </div>
  );
}

export function ButonTrimite({ children, seTrimite }: { children: ReactNode; seTrimite: boolean }) {
  return (
    <button
      type="submit"
      disabled={seTrimite}
      aria-busy={seTrimite}
      className="h-11 w-full rounded-input bg-primary text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {seTrimite ? 'Se trimite…' : children}
    </button>
  );
}

export function ButonLink({ children, onClick }: { children: ReactNode; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} className="text-sm font-semibold text-primary hover:underline">
      {children}
    </button>
  );
}

export function MesajEroare({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-input border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-500">
      {children}
    </p>
  );
}

export function MesajSucces({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="rounded-input border border-accent/40 bg-accent/10 px-3 py-2 text-sm text-ink">
      {children}
    </p>
  );
}

/** Textul de afisat pentru o eroare de API; 429 devine „reîncearcă în N minute”. */
export function mesajEroare(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 429) {
      const minute = Math.max(1, Math.ceil((e.retryAfter ?? 60) / 60));
      return `Prea multe încercări. Reîncearcă în ${minute} ${minute === 1 ? 'minut' : 'minute'}.`;
    }
    return e.detail ?? e.title;
  }
  return 'A apărut o eroare. Reîncearcă.';
}

export function eroriCampuri(e: unknown): Record<string, string> {
  return e instanceof ApiError && e.erori ? e.erori : {};
}

export const PAROLA_MINIMA = 12;
