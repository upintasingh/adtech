package com.adtech.insight.cache;

import com.adtech.insight.dto.MetricType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final StringRedisTemplate redis;

    public Optional<Long> get(String key) {
        String value = redis.opsForValue().get(key);
        return value == null
                ? Optional.empty()
                : Optional.of(Long.parseLong(value));
    }

    public void put(String key, long value, Duration ttl) {
        redis.opsForValue().set(key, String.valueOf(value), ttl);
    }


    public String key(String tenant, String campaignId, MetricType type, Instant from, Instant to) {

        return String.join(":",
                tenant, campaignId, type.name(), from.toString(), to.toString());
    }

    public boolean tryLock(String lockKey, Duration ttl) {
        return redis.opsForValue()
                .setIfAbsent(lockKey, "1", ttl);
    }

    public void unlock(String lockKey) {
        redis.delete(lockKey);
    }
}
