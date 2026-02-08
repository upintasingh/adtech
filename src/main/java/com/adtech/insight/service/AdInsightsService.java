package com.adtech.insight.service;

import com.adtech.insight.cache.RedisCacheService;
import com.adtech.insight.dto.MetricResponse;
import com.adtech.insight.dto.MetricType;
import com.adtech.insight.repository.DynamoMetricsRepository;
import com.adtech.insight.repository.SnowflakeRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdInsightsService {

    @Value("${cache.insight.realtime.ttl}")
    private Duration realtimeTtl;

    @Value("${cache.insight.historical.ttl}")
    private Duration historicalTtl;

    @Value("${cache.insights.lock-ttl}")
    private Duration lockTtl;

    @Value("${insights.realtime.window-hours}")
    private long realtimeWindowHours;

    private final RedisCacheService cache;
    private final DynamoMetricsRepository dynamoRepo;
    private final SnowflakeRepository snowflakeRepo;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @CircuitBreaker(name = "insights", fallbackMethod = "fallback")
    public MetricResponse getMetric(
            String tenant,
            String campaignId,
            MetricType type,
            Instant from,
            Instant to) {

        if (!isRealtime(from, to)) {
            return getHistoricalMetric(tenant, campaignId, type, from, to);
        }

        return getRealtimeMetric(tenant, campaignId, type, from, to);
    }
    private MetricResponse getRealtimeMetric(
            String tenant,
            String campaignId,
            MetricType type,
            Instant from,
            Instant to) {

        String cacheKey = cache.key(tenant, campaignId, type, from, to);
        String lockKey = cacheKey + ":lock";
        CompletableFuture<Optional<Long>> redisFuture =
                CompletableFuture.supplyAsync(
                        () -> cache.get(cacheKey),
                        executor
                );

        CompletableFuture<Long> dynamoFuture =
                CompletableFuture.supplyAsync(
                        () -> dynamoRepo.query(tenant, campaignId, type, from, to),
                        executor
                );
        try {
            Optional<Long> cached = redisFuture.get(25, TimeUnit.MILLISECONDS);
            if (cached.isPresent()) {
                dynamoFuture.cancel(true);
                return new MetricResponse(cached.get());
            }
        } catch (TimeoutException e) {
            log.error("Timeout while waiting for cache key");
            redisFuture.cancel(true);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        long value = dynamoFuture.join();
        boolean lockAcquired = cache.tryLock(lockKey, lockTtl);
        if (lockAcquired) {
            try {
                cache.put(cacheKey, value, realtimeTtl);
            } finally {
                cache.unlock(lockKey);
            }
        }

        return MetricResponse.builder().value(value).build();
    }

    private MetricResponse getHistoricalMetric(
            String tenant,
            String campaignId,
            MetricType type,
            Instant from,
            Instant to) {
        String cacheKey = cache.key(tenant, campaignId, type, from, to);
        String lockKey = cacheKey + ":lock";

        Optional<Long> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return MetricResponse.builder().value(cached.get()).build();
        }

        long value = snowflakeRepo.query(tenant, campaignId, type, from, to);
        boolean lockAcquired = cache.tryLock(lockKey, lockTtl);
        if (lockAcquired) {
            try {
                cache.put(cacheKey, value, historicalTtl);
            } finally {
                cache.unlock(lockKey);
            }
        }

        return MetricResponse.builder().value(value).build();

    }

    private boolean isRealtime(Instant from, Instant to) {
        return Duration.between(from, to).toHours() < realtimeWindowHours;
    }

    MetricResponse fallback(Exception ex) {
        log.error("fallback method called due to: {}", ex.getMessage());
        return MetricResponse.builder().value(0L).build();
    }
}
