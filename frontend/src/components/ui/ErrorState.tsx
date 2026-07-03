interface ErrorStateProps {
  titlu?: string;
  mesaj?: string;
  onRetry?: () => void;
}

export function ErrorState({ titlu = 'A apărut o eroare', mesaj, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-6 py-12 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-accent/10 text-accent dark:bg-accent/15">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.5V13M12 16.5h.01" />
        </svg>
      </div>
      <p className="font-semibold text-ink">{titlu}</p>
      {mesaj && <p className="max-w-sm text-sm text-ink2">{mesaj}</p>}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
        >
          Încearcă din nou
        </button>
      )}
    </div>
  );
}
