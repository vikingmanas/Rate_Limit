package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Rate Limiter.
 *
 * Each key has a bucket that fills with tokens at a steady rate.
 * A request consumes one token. If the bucket is empty the request is rejected.
 * Burst capacity = maxRequests × burstMultiplier, allowing short traffic spikes
 * without violating long-term limits.
 *
 * Thread-safe via ConcurrentHashMap.compute().
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static class Bucket {
        double tokens;
        long lastRefillTimestamp;

        Bucket(double tokens, long timestamp) {
            this.tokens = tokens;
            this.lastRefillTimestamp = timestamp;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult check(String key, RateLimitRule rule) {
        long now = System.currentTimeMillis();
        int burstCapacity = rule.getBurstCapacity();
        // refill rate: maxRequests tokens per windowSeconds
        double refillRatePerMs = (double) rule.getMaxRequests() / (rule.getWindowSeconds() * 1000.0);

        final boolean[] allowed = { false };
        final int[] remaining = { 0 };

        buckets.compute(key, (k, bucket) -> {
            if (bucket == null) {
                // First request — start with a full bucket
                bucket = new Bucket(burstCapacity, now);
            }

            // Refill tokens based on elapsed time
            long elapsed = now - bucket.lastRefillTimestamp;
            if (elapsed > 0) {
                double tokensToAdd = elapsed * refillRatePerMs;
                bucket.tokens = Math.min(burstCapacity, bucket.tokens + tokensToAdd);
                bucket.lastRefillTimestamp = now;
            }

            // Try to consume a token
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                allowed[0] = true;
                remaining[0] = (int) bucket.tokens;
            } else {
                allowed[0] = false;
                remaining[0] = 0;
            }

            return bucket;
        });

        if (allowed[0]) {
            return RateLimitResult.allowed(burstCapacity, remaining[0]);
        } else {
            // Time until one token is refilled
            long retryAfterMs = (long) Math.ceil(1.0 / refillRatePerMs);
            return RateLimitResult.blocked(burstCapacity, retryAfterMs);
        }
    }

    @Override
    public String algorithmName() {
        return "Token Bucket";
    }

    /** Visible for testing — clears all state. */
    public void reset() {
        buckets.clear();
    }
}
