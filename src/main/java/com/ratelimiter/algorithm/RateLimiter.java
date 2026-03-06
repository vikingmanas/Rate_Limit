package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;

/**
 * Core rate limiter interface.
 * All algorithm implementations must be thread-safe.
 */
public interface RateLimiter {

    /**
     * Check whether a request identified by {@code key} is allowed under the given rule.
     *
     * @param key  unique identifier (e.g. API key or IP address)
     * @param rule the rate-limit rule to enforce
     * @return a {@link RateLimitResult} indicating allowed/blocked + metadata
     */
    RateLimitResult check(String key, RateLimitRule rule);

    /**
     * Returns the algorithm name for logging / dashboard display.
     */
    String algorithmName();
}
