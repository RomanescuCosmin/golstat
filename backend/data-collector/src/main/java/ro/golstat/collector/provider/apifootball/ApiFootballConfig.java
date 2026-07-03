package ro.golstat.collector.provider.apifootball;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/** Bean-uri pentru furnizorul real. Ceasul e UTC (cota API-Football se reseteaza la miezul noptii UTC). */
@Configuration
@Profile("!stub")
public class ApiFootballConfig {

    @Bean
    public Clock apiFootballClock() {
        return Clock.systemUTC();
    }
}
