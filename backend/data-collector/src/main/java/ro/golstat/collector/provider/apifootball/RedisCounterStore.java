package ro.golstat.collector.provider.apifootball;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** {@link CounterStore} peste Redis ({@link StringRedisTemplate}). */
@Component
@Profile("!stub")
public class RedisCounterStore implements CounterStore {

    private final StringRedisTemplate redis;

    public RedisCounterStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public long increment(String key) {
        Long value = redis.opsForValue().increment(key);
        return value != null ? value : 0L;
    }

    @Override
    public void expire(String key, Duration ttl) {
        redis.expire(key, ttl);
    }
}
