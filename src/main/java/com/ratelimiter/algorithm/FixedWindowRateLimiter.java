package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed Window Rate Limiter.
 *
 * Divides time into fixed-size windows. Each window has a counter that resets
 * when the window expires. Simple and memory-efficient but can allow up to
 * 2× the limit at window boundaries.
 */
@Component
public class FixedWindowRateLimiter implements RateLimiter {

    /**
     * Holds the counter and the window start timestamp for a given key.
     */
    private static class WindowState {
        final AtomicLong counter = new AtomicLong(0);
        volatile long windowStart;

        WindowState(long windowStart) {
            this.windowStart = windowStart;
        }
    }

    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult check(String key, RateLimitRule rule) {
        long now = System.currentTimeMillis();
        long windowSizeMs = rule.getWindowSeconds() * 1000L;

        WindowState state = windows.compute(key, (k, existing) -> {
            if (existing == null) {
                return new WindowState(now);
            }
            // Reset if window has expired
            if (now - existing.windowStart >= windowSizeMs) {
                existing.counter.set(0);
                existing.windowStart = now;
            }
            return existing;
        });

        long count = state.counter.incrementAndGet();
        int maxRequests = rule.getMaxRequests();

        if (count <= maxRequests) {
            return RateLimitResult.allowed(maxRequests, (int) (maxRequests - count));
        } else {
            long retryAfter = windowSizeMs - (now - state.windowStart);
            return RateLimitResult.blocked(maxRequests, Math.max(retryAfter, 0));
        }
    }

    @Override
    public String algorithmName() {
        return "Fixed Window";
    }

    /** Visible for testing — clears all state. */
    public void reset() {
        windows.clear();
    }
}
