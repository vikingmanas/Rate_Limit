package com.ratelimiter.auth;

import com.ratelimiter.algorithm.RateLimiter;
import com.ratelimiter.algorithm.RateLimiterFactory;
import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import org.springframework.stereotype.Service;

/**
 * Applies rate limiting for unauthenticated (anonymous) requests based on
 * client IP.
 * Uses a stricter default rule.
 */
@Service
public class IpRateLimitService {

    private final RateLimiterFactory factory;

    /** Default IP-based rule: 20 requests per 60 seconds. */
    private volatile RateLimitRule ipRule = new RateLimitRule(20, 60, 1.0, AlgorithmType.TOKEN_BUCKET);

    public IpRateLimitService(RateLimiterFactory factory) {
        this.factory = factory;
    }

    public RateLimitResult check(String clientIp) {
        RateLimiter limiter = factory.getLimiter(ipRule.getAlgorithmType());
        return limiter.check("ip:" + clientIp, ipRule);
    }

    public void updateRule(RateLimitRule rule) {
        this.ipRule = rule;
    }

    public RateLimitRule getRule() {
        return ipRule;
    }
}
