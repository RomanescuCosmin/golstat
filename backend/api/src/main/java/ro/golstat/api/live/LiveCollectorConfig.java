package ro.golstat.api.live;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ro.golstat.collector.live.LiveProperties;
import ro.golstat.collector.provider.apifootball.ApiFootballProperties;

import java.time.Clock;

/**
 * Aduce stratul de furnizor (API-Football) din data-collector in procesul API, ca bucla LIVE sa ruleze
 * intr-un serviciu MEREU pornit — colectorul batch ruleaza one-shot si iese, deci un {@code @Scheduled}
 * de 15s n-are unde sa traiasca acolo.
 *
 * <p>Scaneaza DOAR pachetul {@code provider} (furnizor + client + garda de cota + contor Redis);
 * {@code CollectionPlanner}/{@code OneShotRunner}/{@code LivePoller} din colector stau in alte pachete
 * si NU se activeaza aici — deci colectarea batch nu porneste din greseala in API.
 *
 * <p>Totul e conditionat de {@code golstat.live.enabled=true}: cu live oprit (implicit in teste, care
 * sunt oricum felii {@code @WebMvcTest}) nu se incarca niciun bean de furnizor, deci nici Redis, nici
 * cheia API nu sunt necesare.
 */
@Configuration
@ConditionalOnProperty(name = "golstat.live.enabled", havingValue = "true")
@ComponentScan(basePackages = "ro.golstat.collector.provider")
@EnableConfigurationProperties({ApiFootballProperties.class, LiveProperties.class})
class LiveCollectorConfig {

    /** {@code QuotaGuard} cere un {@link Clock}; colectorul il da prin {@code CollectorConfig}, care nu e scanat aici. */
    @Bean
    Clock liveClock() {
        return Clock.systemUTC();
    }
}
