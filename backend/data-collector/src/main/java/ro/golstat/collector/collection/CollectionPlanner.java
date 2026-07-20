package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.collector.provider.apifootball.QuotaGuard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Declanseaza colectarea, in doua etape cu prioritati diferite:
 *
 * <ol>
 *   <li><b>Zilnic</b> — tintele cu fereastra rulanta (meciuri recente, program, live). Au prioritate
 *       absoluta: sunt ce vede utilizatorul azi.</li>
 *   <li><b>Backfill</b> — tintele cu fereastra absoluta (sezoane trecute). Consuma doar cota ramasa
 *       PESTE {@code golstat.api-football.rezerva-zilnica}, ca un istoric lacom sa nu lase ligile
 *       urmarite fara date.</li>
 * </ol>
 *
 * <p>Cand cota zilnica e atinsa, ciclul se opreste elegant (nu arunca mai departe): ce era deja
 * in cache s-a colectat, restul se reia la urmatorul ciclu dupa reset.
 *
 * <p>Backfill-ul progreseaza intre cicluri prin {@link BackfillCursor}: tintele terminate se sar,
 * deci ciclul urmator continua de unde a ramas in loc sa reia lista de la capat. Fara cursor,
 * ligile de la coada listei nu ar ajunge niciodata la rand — cota s-ar consuma mereu pe primele.
 */
@Component
public class CollectionPlanner {

    private static final Logger log = LoggerFactory.getLogger(CollectionPlanner.class);

    private final CollectionService collection;
    private final CollectionProperties props;
    private final Clock clock;
    private final BackfillCursor cursor;
    private final UltimaRulare ultimaRulare;
    /** Absent pe profilul {@code stub} (fara cota) → backfill-ul ruleaza nelimitat acolo. */
    private final QuotaGuard quota;

    public CollectionPlanner(CollectionService collection, CollectionProperties props, Clock clock,
                             BackfillCursor cursor, UltimaRulare ultimaRulare, @Nullable QuotaGuard quota) {
        this.collection = collection;
        this.props = props;
        this.clock = clock;
        this.cursor = cursor;
        this.ultimaRulare = ultimaRulare;
        this.quota = quota;
    }

    @Scheduled(
            fixedDelayString = "${golstat.collection.interval-ms:3600000}",
            initialDelayString = "${golstat.collection.initial-delay-ms:5000}"
    )
    public void collect() {
        if (props.esteOneShot()) {
            return;   // in one-shot ciclul e pornit de OneShotRunner, o singura data
        }
        ruleazaUnCiclu();
    }

    /** Un ciclu complet: intai zilnicul, apoi cat backfill incape in cota ramasa. */
    public void ruleazaUnCiclu() {
        LocalDate today = LocalDate.now(clock);

        List<LeagueTarget> zilnice = props.leagues().stream().filter(t -> !t.esteBackfill()).toList();
        List<LeagueTarget> backfill = props.leagues().stream().filter(LeagueTarget::esteBackfill).toList();
        long ramaseBackfill = backfill.stream().filter(t -> !cursor.esteGata(t)).count();
        int zileInUrma = ultimaRulare.zileDeAcoperit(props.zileInUrma());
        log.info("Ciclu: {} tinte zilnice (fereastra {} zile in urma), {} tinte backfill ({} neterminate)",
                zilnice.size(), zileInUrma, backfill.size(), ramaseBackfill);

        for (LeagueTarget target : zilnice) {
            if (!colecteaza(target, today, zileInUrma)) {
                return;   // fara marcaj: fereastra n-a fost acoperita, ciclul urmator o reia intreaga
            }
        }
        ultimaRulare.marcheaza();

        for (LeagueTarget target : backfill) {
            if (cursor.esteGata(target)) {
                continue;
            }
            if (quota != null && !quota.bugetPesteRezerva()) {
                log.info("Backfill amanat (cota ramasa {} <= rezerva zilnica); reluam la ciclul urmator",
                        quota.ramase());
                return;
            }
            if (!colecteaza(target, today, zileInUrma)) {
                return;   // tinta ramane NEmarcata → ciclul urmator o reia
            }
            cursor.marcheazaGata(target);
            log.info("Backfill terminat pentru liga {} sezon {}", target.leagueId(), target.season());
        }
    }

    /** {@code false} = cota epuizata, ciclul trebuie oprit (cota e globala pe zi). */
    private boolean colecteaza(LeagueTarget target, LocalDate today, int zileInUrma) {
        LocalDate from = target.from() != null ? target.from() : today.minusDays(zileInUrma);
        LocalDate to = target.to() != null ? target.to() : today.plusDays(props.zileInainte());
        try {
            collection.collectGoalsData(target, from, to);
            return true;
        } catch (ApiFootballQuotaExceededException e) {
            log.warn("Colectare oprita la liga {} (cota atinsa): {}", target.leagueId(), e.getMessage());
            return false;
        }
    }
}
