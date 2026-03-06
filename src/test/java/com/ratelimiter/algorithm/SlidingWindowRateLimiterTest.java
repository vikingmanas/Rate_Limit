package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimiterTest {

    private SlidingWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new SlidingWindowRateLimiter();
    }

    @Test
    @DisplayName("Should allow requests within the limit")
    void shouldAllowWithinLimit() {
        RateLimitRule rule = new RateLimitRule(5, 60, 1.0, AlgorithmType.SLIDING_WINDOW);

        for (int i = 0; i < 5; i++) {
            RateLimitResult result = limiter.check("user-sw-1", rule);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should block requests exceeding the limit")
    void shouldBlockWhenExceeded() {
        RateLimitRule rule = new RateLimitRule(3, 60, 1.0, AlgorithmType.SLIDING_WINDOW);

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.check("user-sw-2", rule).isAllowed());
        }

        RateLimitResult blocked = limiter.check("user-sw-2", rule);
        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getRetryAfterMillis() > 0);
    }

    @Test
    @DisplayName("Should track precise remaining count")
    void shouldTrackRemaining() {
        RateLimitRule rule = new RateLimitRule(4, 60, 1.0, AlgorithmType.SLIDING_WINDOW);

        assertEquals(3, limiter.check("user-sw-rem", rule).getRemaining());
        assertEquals(2, limiter.check("user-sw-rem", rule).getRemaining());
        assertEquals(1, limiter.check("user-sw-rem", rule).getRemaining());
        assertEquals(0, limiter.check("user-sw-rem", rule).getRemaining());
    }

    @Test
    @DisplayName("Different keys should be independent")
    void independentKeys() {
        RateLimitRule rule = new RateLimitRule(1, 60, 1.0, AlgorithmType.SLIDING_WINDOW);

        assertTrue(limiter.check("key-x", rule).isAllowed());
        assertFalse(limiter.check("key-x", rule).isAllowed());

        // Different key
        assertTrue(limiter.check("key-y", rule).isAllowed());
    }

    @Test
    @DisplayName("Algorithm name should be Sliding Window")
    void algorithmName() {
        assertEquals("Sliding Window", limiter.algorithmName());
    }
}
