package ro.golstat.collector.publish;

/**
 * Abstractizarea publicarii in Kafka. Exista ca sa putem testa colectarea (ce/unde publica)
 * fara un broker real — implementarea de productie e {@link KafkaEventPublisher}.
 */
public interface EventPublisher {

    /** Publica {@code payload} pe {@code topic} cu cheia data (id-ul entitatii). */
    void publish(String topic, String key, Object payload);
}
