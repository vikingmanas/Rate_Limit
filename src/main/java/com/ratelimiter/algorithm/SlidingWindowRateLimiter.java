package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding Window Log Rate Limiter.
 *
 * Maintains a log of request timestamps per key and counts how many fall
 * within the current sliding window. More accurate than fixed-window but
 * uses more memory because it stores individual timestamps.
 */
@Component
public class SlidingWindowRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requestLogs = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult check(String key, RateLimitRule rule) {
        long now = System.currentTimeMillis();
        long windowMs = rule.getWindowSeconds() * 1000L;
        long windowStart = now - windowMs;

        ConcurrentLinkedDeque<Long> log = requestLogs.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Evict expired entries from the head of the deque
        while (!log.isEmpty()) {
            Long oldest = log.peekFirst();
            if (oldest != null && oldest <= windowStart) {
                log.pollFirst();
            } else {
                break;
            }
        }

        int currentCount = log.size();
        int maxRequests = rule.getMaxRequests();

        if (currentCount < maxRequests) {
            log.addLast(now);
            return RateLimitResult.allowed(maxRequests, maxRequests - currentCount - 1);
        } else {
            // Retry after the oldest entry in the window expires
            Long oldest = log.peekFirst();
            long retryAfter = oldest != null ? (oldest + windowMs - now) : windowMs;
            return RateLimitResult.blocked(maxRequests, Math.max(retryAfter, 0));
        }
    }

    @Override
    public String algorithmName() {
        return "Sliding Window";
    }

    /** Visible for testing — clears all state. */
    public void reset() {
        requestLogs.clear();
    }
}
