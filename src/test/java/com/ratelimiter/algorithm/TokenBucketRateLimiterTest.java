package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new TokenBucketRateLimiter();
    }

    @Test
    @DisplayName("Should allow requests up to burst capacity")
    void shouldAllowUpToBurstCapacity() {
        // burstMultiplier=2.0 → burstCapacity = 5 * 2 = 10
        RateLimitRule rule = new RateLimitRule(5, 60, 2.0, AlgorithmType.TOKEN_BUCKET);

        int allowed = 0;
        for (int i = 0; i < 12; i++) {
            if (limiter.check("tb-user-1", rule).isAllowed()) {
                allowed++;
            }
        }
        // Should be exactly 10 (burst capacity)
        assertEquals(10, allowed);
    }

    @Test
    @DisplayName("Should block when tokens exhausted")
    void shouldBlockWhenEmpty() {
        RateLimitRule rule = new RateLimitRule(3, 60, 1.0, AlgorithmType.TOKEN_BUCKET);

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.check("tb-user-2", rule).isAllowed());
        }

        RateLimitResult blocked = limiter.check("tb-user-2", rule);
        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getRetryAfterMillis() > 0);
    }

    @Test
    @DisplayName("Should report remaining tokens")
    void shouldReportRemaining() {
        RateLimitRule rule = new RateLimitRule(5, 60, 1.0, AlgorithmType.TOKEN_BUCKET);

        RateLimitResult r1 = limiter.check("tb-rem", rule);
        assertTrue(r1.isAllowed());
        assertEquals(4, r1.getRemaining());
    }

    @Test
    @DisplayName("Different keys should have independent buckets")
    void independentBuckets() {
        RateLimitRule rule = new RateLimitRule(2, 60, 1.0, AlgorithmType.TOKEN_BUCKET);

        assertTrue(limiter.check("bucket-a", rule).isAllowed());
        assertTrue(limiter.check("bucket-a", rule).isAllowed());
        assertFalse(limiter.check("bucket-a", rule).isAllowed());

        assertTrue(limiter.check("bucket-b", rule).isAllowed());
    }

    @Test
    @DisplayName("Burst multiplier should increase capacity")
    void burstMultiplierIncreasesCapacity() {
        RateLimitRule noBurst = new RateLimitRule(5, 60, 1.0, AlgorithmType.TOKEN_BUCKET);
        RateLimitRule withBurst = new RateLimitRule(5, 60, 3.0, AlgorithmType.TOKEN_BUCKET);

        assertEquals(5, noBurst.getBurstCapacity());
        assertEquals(15, withBurst.getBurstCapacity());
    }

    @Test
    @DisplayName("Algorithm name should be Token Bucket")
    void algorithmName() {
        assertEquals("Token Bucket", limiter.algorithmName());
    }
}
