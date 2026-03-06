package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that resolves the correct {@link RateLimiter} implementation
 * for a given {@link AlgorithmType}.
 */
@Component
public class RateLimiterFactory {

    private final Map<AlgorithmType, RateLimiter> limiters;

    public RateLimiterFactory(FixedWindowRateLimiter fixedWindow,
            SlidingWindowRateLimiter slidingWindow,
            TokenBucketRateLimiter tokenBucket) {
        limiters = new EnumMap<>(AlgorithmType.class);
        limiters.put(AlgorithmType.FIXED_WINDOW, fixedWindow);
        limiters.put(AlgorithmType.SLIDING_WINDOW, slidingWindow);
        limiters.put(AlgorithmType.TOKEN_BUCKET, tokenBucket);
    }

    public RateLimiter getLimiter(AlgorithmType type) {
        RateLimiter limiter = limiters.get(type);
        if (limiter == null) {
            throw new IllegalArgumentException("Unsupported algorithm type: " + type);
        }
        return limiter;
    }
}
