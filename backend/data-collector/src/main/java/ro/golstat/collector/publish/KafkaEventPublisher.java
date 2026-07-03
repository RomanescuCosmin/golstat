package ro.golstat.collector.publish;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publicare reala in Kafka: cheia = String, valoarea = DTO serializat JSON. */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(String topic, String key, Object payload) {
        kafka.send(topic, key, payload);
    }
}
