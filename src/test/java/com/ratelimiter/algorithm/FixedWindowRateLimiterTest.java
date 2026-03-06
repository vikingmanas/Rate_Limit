package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedWindowRateLimiterTest {

    private FixedWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new FixedWindowRateLimiter();
    }

    @Test
    @DisplayName("Should allow requests within the limit")
    void shouldAllowWithinLimit() {
        RateLimitRule rule = new RateLimitRule(5, 60, 1.0, AlgorithmType.FIXED_WINDOW);

        for (int i = 0; i < 5; i++) {
            RateLimitResult result = limiter.check("user-1", rule);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should block requests exceeding the limit")
    void shouldBlockWhenLimitExceeded() {
        RateLimitRule rule = new RateLimitRule(3, 60, 1.0, AlgorithmType.FIXED_WINDOW);

        // Use up all 3 requests
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.check("user-2", rule).isAllowed());
        }

        // 4th request should be blocked
        RateLimitResult result = limiter.check("user-2", rule);
        assertFalse(result.isAllowed());
        assertTrue(result.getRetryAfterMillis() > 0);
    }

    @Test
    @DisplayName("Different keys should have independent limits")
    void differentKeysShouldBeIndependent() {
        RateLimitRule rule = new RateLimitRule(2, 60, 1.0, AlgorithmType.FIXED_WINDOW);

        assertTrue(limiter.check("user-a", rule).isAllowed());
        assertTrue(limiter.check("user-a", rule).isAllowed());
        assertFalse(limiter.check("user-a", rule).isAllowed());

        // Different key should still be allowed
        assertTrue(limiter.check("user-b", rule).isAllowed());
    }

    @Test
    @DisplayName("Should report correct remaining count")
    void shouldReportRemaining() {
        RateLimitRule rule = new RateLimitRule(5, 60, 1.0, AlgorithmType.FIXED_WINDOW);

        RateLimitResult r1 = limiter.check("user-r", rule);
        assertEquals(4, r1.getRemaining());

        RateLimitResult r2 = limiter.check("user-r", rule);
        assertEquals(3, r2.getRemaining());
    }

    @Test
    @DisplayName("Algorithm name should be Fixed Window")
    void algorithmName() {
        assertEquals("Fixed Window", limiter.algorithmName());
    }
}
