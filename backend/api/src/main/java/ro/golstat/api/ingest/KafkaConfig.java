package ro.golstat.api.ingest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Tratarea erorilor de consum. Reincearca cu BACKOFF (nu instant), ca race-urile tranzitorii dintre
 * containerele paralele — un parinte inca ne-comis pe alt topic (ex. meciul inca neingerat cand vin
 * evenimentele; aceeasi echipa inserata concurent din doua topicuri) — sa se rezolve la retry. Dupa
 * epuizarea incercarilor, logheaza si SARE peste inregistrare, ca sa nu blocheze partitia la infinit.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 8L));
    }
}
