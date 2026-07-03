package ro.golstat.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Serviciul de colectare: cere date de la furnizor (API-Football), le publica in Kafka.
 * NU atinge Postgres — singura lui stare e Redis (cota + cache). Scheduler-ul (CollectionPlanner)
 * intinde colectarea pe zi; de-aceea {@code @EnableScheduling}.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class DataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCollectorApplication.class, args);
    }
}
