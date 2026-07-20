package ro.golstat.collector.collection;

import java.time.LocalDate;

/**
 * O tinta de colectare: o competitie intr-un sezon anume (ex. CM 2026, Premier League 2025).
 *
 * <p>{@code doarFixtures} = colecteaza NUMAI meciurile (pentru afisare), fara detalii per-meci
 * (formatii/evenimente/statistici) si fara imbogatirea echipelor (jucatori/statistici sezon) —
 * pentru competitii cu foarte multe meciuri unde ne intereseaza doar scorul (ex. amicale), ca sa
 * nu ardem cota API. Implicit {@code false}.
 *
 * <p>{@code imbogatireEchipe} (implicit {@code true}) = aduce lotul, statisticile de sezon si
 * antrenorul FIECAREI echipe din competitie, plus indisponibilii. Costa ~4 requesturi per echipa,
 * deci pe competitiile cu mii de echipe (amicalele au ~2100) depaseste singura cota zilnica: acolo
 * il stingem si pastram doar detaliile per meci.
 *
 * <p>{@code statisticiJucatori} = cere si notele/statisticile individuale din meci
 * ({@code /fixtures/players}). Implicit {@code false}: costa inca 1 request per meci si furnizorul
 * are date doar pe unele competitii (vezi {@code coverage.statistics_players}).
 *
 * <p>{@code doarStatisticiJucatori} = pentru ligile deja colectate: cere DOAR {@code /fixtures/players},
 * sarind formatiile/evenimentele/statisticile de echipa. Colectorul nu stie ce e deja in Postgres, iar
 * cache-ul Redis expira in 24h — fara acest mod, o recolectare doar pentru note ar re-cere si restul
 * detaliilor (3 requesturi in plus per meci). Implica {@code statisticiJucatori}.
 *
 * <p>{@code from}/{@code to} = fereastra ABSOLUTA de meciuri, pentru backfill istoric (ex. sezonul
 * 2025: {@code 2025-07-01}..{@code 2026-06-30}). Absente → planner-ul foloseste fereastra RULANTA
 * globala din {@code golstat.collection.zile-in-urma/zile-inainte}. Distinctia conteaza: fara ea,
 * un backfill pe un sezon trecut ar cere umflarea ferestrei pentru toate ligile urmarite, deci
 * re-colectarea inutila a tot istoricul lor la fiecare ciclu.
 */
public record LeagueTarget(long leagueId, int season, boolean doarFixtures, Boolean imbogatireEchipe,
                           boolean statisticiJucatori, boolean doarStatisticiJucatori,
                           LocalDate from, LocalDate to) {

    public LeagueTarget {
        // absent in YAML → pornit (spre deosebire de flagurile opt-in, care sunt primitive)
        if (imbogatireEchipe == null) {
            imbogatireEchipe = true;
        }
        if (doarStatisticiJucatori) {
            statisticiJucatori = true;
        }
    }

    /**
     * Tinta zilnica (fereastra rulanta). Deliberat factory, nu al doilea constructor — cu doua
     * constructoare Spring nu mai stie pe care sa lege si binding-ul din YAML crapa la pornire.
     */
    public static LeagueTarget zilnica(long leagueId, int season, boolean doarFixtures,
                                       Boolean imbogatireEchipe, boolean statisticiJucatori,
                                       boolean doarStatisticiJucatori) {
        return new LeagueTarget(leagueId, season, doarFixtures, imbogatireEchipe, statisticiJucatori,
                doarStatisticiJucatori, null, null);
    }

    /**
     * Tinta de backfill = are fereastra proprie. Ajunge o singura margine: o tinta cu doar {@code from}
     * ramane istorica (colecteaza pana azi + {@code zileInainte}).
     */
    public boolean esteBackfill() {
        return from != null || to != null;
    }

    /** Cheie stabila pentru cursorul de progres si pentru loguri. */
    public String cheie() {
        return leagueId + ":" + season;
    }
}
