const TABURI = ['Prezentare', 'Rezultate', 'Meciuri', 'Statistici', 'Jucători', 'Transferuri'] as const;

/** Tab-urile paginii echipei; doar "Prezentare" e activ, restul "În curând" (ca placeholderele din TopNav). */
export function TabsEchipa() {
  return (
    <div className="flex gap-6 overflow-x-auto border-b border-line sm:gap-8" role="tablist">
      {TABURI.map((tab, i) => {
        const activ = i === 0;
        return (
          <span
            key={tab}
            role="tab"
            aria-selected={activ}
            title={activ ? undefined : 'În curând'}
            className={`flex h-11 items-center whitespace-nowrap border-b-2 text-sm transition duration-200 ${
              activ
                ? 'border-primary font-semibold text-primary'
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
