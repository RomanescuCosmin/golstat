package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Ruleaza UN ciclu de colectare si opreste procesul cu exit code 0.
 *
 * <p>Pentru rularea din Windows Task Scheduler: PC-ul e trezit din sleep la interval fix, colecteaza
 * si adoarme la loc. Un proces care ramane pornit (modul {@code scheduled}) ar tine masina treaza.
 *
 * <p>Iesirea e explicita pentru ca Kafka producer-ul tine fire non-daemon: fara {@code exit},
 * procesul ar ramane agatat dupa terminarea ciclului, iar scriptul de trezire n-ar continua.
 */
@Component
public class OneShotRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OneShotRunner.class);

    private final CollectionPlanner planner;
    private final CollectionProperties props;
    private final ConfigurableApplicationContext ctx;

    public OneShotRunner(CollectionPlanner planner, CollectionProperties props, ConfigurableApplicationContext ctx) {
        this.planner = planner;
        this.props = props;
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.esteOneShot()) {
            return;
        }
        log.info("Mod one-shot: rulez un ciclu de colectare, apoi ies");
        int cod;
        try {
            planner.ruleazaUnCiclu();
            log.info("Ciclu one-shot terminat");
            cod = 0;
        } catch (RuntimeException e) {
            // exit != 0 ca scriptul de trezire sa poata loga esecul, nu sa creada ca a mers
            log.error("Ciclu one-shot esuat", e);
            cod = 1;
        }
        int codFinal = cod;
        SpringApplication.exit(ctx, () -> codFinal);
        System.exit(codFinal);
    }
}
