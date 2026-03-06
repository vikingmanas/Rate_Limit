package com.ratelimiter.redis;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Redis-backed distributed rate limit store.
 * Uses Lua scripts for atomic operations.
 * Falls back gracefully when Redis is unavailable.
 */
@Component
public class RedisRateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStore.class);

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<List> fixedWindowScript;
    private DefaultRedisScript<List> slidingWindowScript;
    private DefaultRedisScript<List> tokenBucketScript;

    private volatile boolean redisAvailable = true;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        fixedWindowScript = new DefaultRedisScript<>();
        fixedWindowScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/fixed_window.lua")));
        fixedWindowScript.setResultType(List.class);

        slidingWindowScript = new DefaultRedisScript<>();
        slidingWindowScript
                .setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/sliding_window.lua")));
        slidingWindowScript.setResultType(List.class);

        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua")));
        tokenBucketScript.setResultType(List.class);
    }

    /**
     * Attempts to perform a rate limit check via Redis.
     * Returns null if Redis is unavailable (caller should fall back to in-memory).
     */
    public RateLimitResult check(String key, RateLimitRule rule, AlgorithmType algorithmType) {
        if (!redisAvailable) {
            return null; // signal caller to use in-memory fallback
        }

        try {
            return switch (algorithmType) {
                case FIXED_WINDOW -> checkFixedWindow(key, rule);
                case SLIDING_WINDOW -> checkSlidingWindow(key, rule);
                case TOKEN_BUCKET -> checkTokenBucket(key, rule);
            };
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, falling back to in-memory rate limiting: {}", e.getMessage());
            redisAvailable = false;
            return null;
        } catch (Exception e) {
            log.error("Redis rate limit check failed: {}", e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private RateLimitResult checkFixedWindow(String key, RateLimitRule rule) {
        String redisKey = "rl:fw:" + key;
        List<Long> result = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(redisKey),
                String.valueOf(rule.getWindowSeconds()),
                String.valueOf(rule.getMaxRequests()));

        if (result == null || result.size() < 2)
            return null;

        long count = result.get(0);
        long ttl = result.get(1);
        int max = rule.getMaxRequests();

        if (count <= max) {
            return RateLimitResult.allowed(max, (int) (max - count));
        } else {
            return RateLimitResult.blocked(max, ttl * 1000L);
        }
    }

    @SuppressWarnings("unchecked")
    private RateLimitResult checkSlidingWindow(String key, RateLimitRule rule) {
        String redisKey = "rl:sw:" + key;
        long now = System.currentTimeMillis();
        long windowMs = rule.getWindowSeconds() * 1000L;
        String requestId = UUID.randomUUID().toString();

        List<Long> result = redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(redisKey),
                String.valueOf(now),
                String.valueOf(windowMs),
                String.valueOf(rule.getMaxRequests()),
                requestId);

        if (result == null || result.size() < 2)
            return null;

        long count = result.get(0);
        long allowed = result.get(1);
        int max = rule.getMaxRequests();

        if (allowed == 1) {
            return RateLimitResult.allowed(max, (int) (max - count));
        } else {
            return RateLimitResult.blocked(max, windowMs);
        }
    }

    @SuppressWarnings("unchecked")
    private RateLimitResult checkTokenBucket(String key, RateLimitRule rule) {
        String tokenKey = "rl:tb:tokens:" + key;
        String tsKey = "rl:tb:ts:" + key;
        long now = System.currentTimeMillis();
        int capacity = rule.getBurstCapacity();
        double refillRate = (double) rule.getMaxRequests() / (rule.getWindowSeconds() * 1000.0);
        long windowMs = rule.getWindowSeconds() * 1000L;

        List<Long> result = redisTemplate.execute(
                tokenBucketScript,
                Arrays.asList(tokenKey, tsKey),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(now),
                String.valueOf(windowMs));

        if (result == null || result.size() < 2)
            return null;

        long allowed = result.get(0);
        long remaining = result.get(1);

        if (allowed == 1) {
            return RateLimitResult.allowed(capacity, (int) remaining);
        } else {
            long retryAfterMs = (long) Math.ceil(1.0 / refillRate);
            return RateLimitResult.blocked(capacity, retryAfterMs);
        }
    }

    /** Re-check Redis connectivity (can be called periodically). */
    public void recheckRedis() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            if (!redisAvailable) {
                log.info("Redis connection restored.");
            }
            redisAvailable = true;
        } catch (Exception e) {
            redisAvailable = false;
        }
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}
