package com.contaplus.api.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final RateLimitConfig config;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitConfig config) {
        this.config = config;
    }

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, this::createStandardBucket);
    }

    public Bucket resolveAuthBucket(String key) {
        return authBuckets.computeIfAbsent(key, this::createAuthBucket);
    }

    private Bucket createStandardBucket(String key) {
        Bandwidth limit = Bandwidth.classic(
            config.getRequestsPerMinute(),
            Refill.greedy(config.getRequestsPerMinute(), Duration.ofMinutes(1))
        );

        Bandwidth burst = Bandwidth.classic(
            config.getBurstCapacity(),
            Refill.intervally(config.getBurstCapacity(), Duration.ofSeconds(10))
        );

        return Bucket.builder()
            .addLimit(limit)
            .addLimit(burst)
            .build();
    }

    private Bucket createAuthBucket(String key) {
        Bandwidth limit = Bandwidth.classic(
            config.getAuthRequestsPerMinute(),
            Refill.greedy(config.getAuthRequestsPerMinute(), Duration.ofMinutes(1))
        );

        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void clearBuckets() {
        buckets.clear();
        authBuckets.clear();
    }
}
