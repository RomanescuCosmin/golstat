package ro.golstat.collector.collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica direct fisierul {@code application-oneshot.yml}. O greseala de tipar acolo (indentare,
 * {@code from} scris gresit, data invalida) nu s-ar vedea la compilare si ar aparea abia noaptea,
 * cand task-ul programat ruleaza fara nimeni la calculator.
 */
class OneshotProfileConfigTest {

    private static CollectionProperties incarca() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-oneshot.yml"));
        Properties properties = yaml.getObject();

        StandardEnvironment env = new StandardEnvironment();
        PropertySource<?> source = new PropertiesPropertySource("oneshot", properties);
        env.getPropertySources().addFirst(source);

        return Binder.get(env).bind("golstat.collection", CollectionProperties.class).get();
    }

    @Test
    void yamlBindsToCollectionProperties() {
        CollectionProperties props = incarca();

        assertTrue(props.esteOneShot(), "profilul oneshot trebuie sa ruleze un singur ciclu");
        // 1 zi e sigur DOAR fiindca UltimaRulare largeste fereastra dupa o pauza
        assertEquals(1, props.zileInUrma());
        // ProgramPage cere 7 zile de program; lookahead-ul nu costa requesturi in plus
        assertEquals(30, props.zileInainte());
    }

    @Test
    void dailyTargetsHaveNoAbsoluteWindow() {
        List<LeagueTarget> zilnice = incarca().leagues().stream()
                .filter(t -> !t.esteBackfill())
                .toList();

        assertEquals(26, zilnice.size(), "cate competitii are si frontend/src/lib/ligi.ts");
        assertTrue(zilnice.stream().allMatch(t -> t.season() == 2026), "zilnicul urmareste sezonul curent");
        assertTrue(zilnice.stream().anyMatch(t -> t.leagueId() == 667 && t.doarFixtures()),
                "amicalele raman doar-fixtures, altfel imbogatirea depaseste singura cota zilnica");
        // ligile mari trebuie urmarite si zilnic, nu doar aduse istoric prin backfill
        assertTrue(zilnice.stream().map(LeagueTarget::leagueId).toList()
                        .containsAll(List.of(39L, 140L, 135L, 78L, 61L)),
                "top 5 lipseau din lista zilnica: sezonul curent n-ar fi colectat deloc");
    }

    @Test
    void backfillTargetsAreParsedWithDatesAndAreDistinctFromDailyOnes() {
        List<LeagueTarget> backfill = incarca().leagues().stream()
                .filter(LeagueTarget::esteBackfill)
                .toList();

        assertEquals(14, backfill.size(), "12 ligi pe sezonul trecut + 2 recuperari pe sezonul curent");
        for (LeagueTarget t : backfill) {
            assertTrue(t.imbogatireEchipe(), "loturile sunt necesare pentru statisticile jucatorilor");
            assertFalse(t.statisticiJucatori(), "notele per meci ar adauga ~380 requesturi per liga");
        }
        for (LeagueTarget t : backfill.stream().filter(x -> x.season() == 2025).toList()) {
            assertEquals(LocalDate.of(2025, 1, 1), t.from());
            assertEquals(LocalDate.of(2026, 6, 30), t.to());
        }

        // Cheia e liga+sezon: 113 apare legitim de doua ori (2025 istoric + 2026 recuperare),
        // dar aceeasi pereche de doua ori ar insemna colectare dubla.
        List<String> chei = backfill.stream().map(LeagueTarget::cheie).toList();
        assertEquals(chei.size(), chei.stream().distinct().count(), "tinta de backfill duplicata");
        assertTrue(backfill.stream().map(LeagueTarget::leagueId).toList()
                .containsAll(List.of(94L, 203L, 136L, 141L)), "ligile inca neaduse");
    }

    @Test
    void alreadyCollectedLeaguesAreNotScheduledAgain() {
        // Sezonul 2025 exista deja in Postgres pentru ligile astea (verificat 2026-07-20). Re-colectarea
        // ar costa ~9600 requesturi degeaba, adica peste cota unei zile intregi.
        List<Long> dejaColectate = List.of(39L, 140L, 135L, 40L, 144L, 61L, 88L, 78L, 283L, 218L);

        List<Long> backfill = incarca().leagues().stream()
                .filter(LeagueTarget::esteBackfill)
                .map(LeagueTarget::leagueId)
                .toList();

        assertTrue(backfill.stream().noneMatch(dejaColectate::contains),
                "backfill programat pentru o liga deja colectata: " + backfill);
    }

    @Test
    void midSeasonLeaguesHaveACurrentSeasonCatchUpTarget() {
        // Fereastra rulanta prinde doar de azi inainte. O liga adaugata in mijlocul sezonului isi pierde
        // definitiv meciurile deja jucate daca nu primeste si o tinta cu fereastra absoluta.
        // Allsvenskan si Eliteserien joaca pe an calendaristic, deci in iulie sunt la ~2/3 de sezon.
        List<LeagueTarget> backfill = incarca().leagues().stream()
                .filter(LeagueTarget::esteBackfill)
                .toList();

        for (long liga : List.of(113L, 103L)) {
            assertTrue(backfill.stream().anyMatch(t -> t.leagueId() == liga && t.season() == 2026),
                    "liga " + liga + " e in plin sezon 2026 dar n-are tinta de recuperare");
        }
    }

    @Test
    void catchUpTargetsAreAlsoTrackedDaily() {
        // Recuperarea aduce istoricul o singura data si se marcheaza gata; fara tinta zilnica pe
        // aceeasi liga+sezon, meciurile de dupa recuperare n-ar mai intra niciodata.
        List<LeagueTarget> toate = incarca().leagues();
        List<String> zilnice = toate.stream().filter(t -> !t.esteBackfill()).map(LeagueTarget::cheie).toList();

        toate.stream()
                .filter(t -> t.esteBackfill() && t.season() == 2026)
                .forEach(t -> assertTrue(zilnice.contains(t.cheie()),
                        "recuperare pe " + t.cheie() + " fara tinta zilnica pereche"));
    }

    @Test
    void everyBackfillLeagueIsAlsoTrackedDaily() {
        List<LeagueTarget> toate = incarca().leagues();
        List<Long> zilnice = toate.stream().filter(t -> !t.esteBackfill()).map(LeagueTarget::leagueId).toList();

        // Altfel am aduce istoricul unei ligi si i-am lasa sezonul curent sa se invecheasca — exact
        // capcana in care intrase configul initial.
        toate.stream().filter(LeagueTarget::esteBackfill).forEach(t ->
                assertTrue(zilnice.contains(t.leagueId()),
                        "liga " + t.leagueId() + " are backfill dar nu e urmarita zilnic"));
    }
}
