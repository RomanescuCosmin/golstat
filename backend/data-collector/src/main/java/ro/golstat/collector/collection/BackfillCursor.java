package ro.golstat.collector.collection;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.apifootball.CounterStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tine minte ce tinte de backfill s-au terminat, ca sa nu le reluam.
 *
 * <p>De ce e nevoie: colectorul nu citeste niciodata din Postgres, iar singura lui "memorie" e
 * cache-ul Redis per-request, care expira in 24h ({@code ttlHistoric}). Fara un marcaj permanent,
 * un sezon adus alaltaieri s-ar re-cere integral la urmatorul ciclu — adica ~1200 de requesturi
 * aruncate per liga, la fiecare rulare.
 *
 * <p>Marcam DOAR terminarea completa. O tinta intrerupta la mijloc (cota epuizata) ramane
 * nemarcata si se reia de la inceput, iar cache-ul Redis face reluarea aproape gratuita cat timp
 * se intampla in aceeasi zi.
 */
@Component
public class BackfillCursor {

    private static final String PREFIX = "golstat:backfill:";
    private static final String GATA = "DONE";

    /** Absent pe profilul {@code stub} (Redis nu exista acolo) → cadem pe memorie de proces. */
    private final CounterStore store;
    private final Map<String, String> inMemory = new ConcurrentHashMap<>();

    public BackfillCursor(@Nullable CounterStore store) {
        this.store = store;
    }

    public boolean esteGata(LeagueTarget target) {
        String cheie = PREFIX + target.cheie();
        Optional<String> valoare = store != null ? store.get(cheie) : Optional.ofNullable(inMemory.get(cheie));
        return valoare.filter(GATA::equals).isPresent();
    }

    public void marcheazaGata(LeagueTarget target) {
        String cheie = PREFIX + target.cheie();
        if (store != null) {
            store.set(cheie, GATA);
        } else {
            inMemory.put(cheie, GATA);
        }
    }
}
