package com.andersonvianadev.finance_control_api.infra.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;
    private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    public RateLimitResult tryConsumeGlobal(String clientIp) {
        return tryConsume(globalBuckets, clientIp, properties.global().requestsPerMinute());
    }

    public RateLimitResult tryConsumeAuth(String clientIp) {
        return tryConsume(authBuckets, clientIp, properties.auth().requestsPerMinute());
    }

    public void resetBuckets() {
        globalBuckets.clear();
        authBuckets.clear();
    }

    private RateLimitResult tryConsume(Map<String, Bucket> buckets, String clientIp, int requestsPerMinute) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, key -> createBucket(requestsPerMinute));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        long resetEpochSeconds = Instant.now().getEpochSecond()
                + Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());

        return new RateLimitResult(
                probe.isConsumed(),
                requestsPerMinute,
                probe.getRemainingTokens(),
                resetEpochSeconds
        );
    }

    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
