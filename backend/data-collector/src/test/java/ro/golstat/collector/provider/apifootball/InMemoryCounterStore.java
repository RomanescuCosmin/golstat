package ro.golstat.collector.provider.apifootball;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** {@link CounterStore} in-memory pentru teste: contor real (prinde bug-uri de numarare), TTL retinut. */
public class InMemoryCounterStore implements CounterStore {

    final Map<String, String> values = new HashMap<>();
    final Map<String, Duration> ttls = new HashMap<>();

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        values.put(key, value);
        ttls.put(key, ttl);
    }

    @Override
    public void set(String key, String value) {
        values.put(key, value);
    }

    @Override
    public long increment(String key) {
        long value = Long.parseLong(values.getOrDefault(key, "0")) + 1;
        values.put(key, Long.toString(value));
        return value;
    }

    @Override
    public void expire(String key, Duration ttl) {
        ttls.put(key, ttl);
    }
}
