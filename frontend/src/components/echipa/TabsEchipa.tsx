const TABURI = ['Prezentare', 'Rezultate', 'Meciuri', 'Statistici', 'Jucători', 'Transferuri'] as const;

/** Tab-urile paginii echipei; doar "Prezentare" e activ, restul "În curând" (ca placeholderele din TopNav). */
export function TabsEchipa() {
  return (
    <div className="flex items-center gap-1 overflow-x-auto border-b border-line">
      {TABURI.map((tab, i) => {
        const activ = i === 0;
        return (
          <span
            key={tab}
            aria-current={activ ? 'page' : undefined}
            title={activ ? undefined : 'În curând'}
            className={`whitespace-nowrap border-b-2 px-3 py-2.5 text-sm font-semibold transition ${
              activ
                ? 'border-primary text-primary'
                : 'cursor-not-allowed border-transparent text-ink2/60'
            }`}
          >
            {tab}
          </span>
        );
      })}
    </div>
  );
}
