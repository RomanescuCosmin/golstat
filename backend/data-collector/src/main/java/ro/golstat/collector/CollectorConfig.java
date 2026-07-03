package ro.golstat.collector;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Bean-uri comune colectorului. Ceas UTC partajat (fereastra de colectare + garda de cota); fix in teste. */
@Configuration
public class CollectorConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
