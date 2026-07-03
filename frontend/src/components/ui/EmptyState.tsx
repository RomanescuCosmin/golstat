interface EmptyStateProps {
  titlu?: string;
  mesaj?: string;
}

export function EmptyState({ titlu = 'Nimic de afișat', mesaj }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-6 py-12 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-ink2/10 text-ink2 dark:bg-ink2/15">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden>
          <circle cx="12" cy="12" r="9" />
          <path d="M8 13.5c1 1.2 2.4 2 4 2s3-.8 4-2M9 9.5h.01M15 9.5h.01" />
        </svg>
      </div>
      <p className="font-semibold text-ink">{titlu}</p>
      {mesaj && <p className="max-w-sm text-sm text-ink2">{mesaj}</p>}
    </div>
  );
}
