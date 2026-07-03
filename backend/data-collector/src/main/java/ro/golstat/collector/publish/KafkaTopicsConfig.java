package ro.golstat.collector.publish;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import ro.golstat.common.GolstatConstants;

/**
 * Topicurile de entitati folosesc {@code cleanup.policy=compact}: cheia mesajului e id-ul
 * entitatii, deci Kafka pastreaza ultima stare per entitate — replay-ul reconstruieste DB-ul
 * complet si ieftin. O partitie / o replica pentru dev local.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic fixturesTopic() {
        return TopicBuilder.name(GolstatConstants.KafkaTopics.FIXTURES).partitions(1).replicas(1).compact().build();
    }

    @Bean
    NewTopic fixtureEventsTopic() {
        return TopicBuilder.name(GolstatConstants.KafkaTopics.FIXTURE_EVENTS).partitions(1).replicas(1).compact().build();
    }

    @Bean
    NewTopic standingsTopic() {
        return TopicBuilder.name(GolstatConstants.KafkaTopics.STANDINGS).partitions(1).replicas(1).compact().build();
    }

    @Bean
    NewTopic teamsTopic() {
        return TopicBuilder.name(GolstatConstants.KafkaTopics.TEAMS).partitions(1).replicas(1).compact().build();
    }
}
