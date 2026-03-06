package com.ratelimiter.interceptor;

import com.ratelimiter.admin.RuleConfigService;
import com.ratelimiter.algorithm.RateLimiter;
import com.ratelimiter.algorithm.RateLimiterFactory;
import com.ratelimiter.auth.ApiKeyAuthFilter;
import com.ratelimiter.auth.IpRateLimitService;
import com.ratelimiter.model.*;
import com.ratelimiter.monitoring.MetricsService;
import com.ratelimiter.redis.RedisRateLimitStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring MVC interceptor that enforces rate limiting on incoming requests.
 *
 * <p>
 * Flow:
 * </p>
 * <ol>
 * <li>Check if request has an authenticated API key (set by
 * {@link ApiKeyAuthFilter}).</li>
 * <li>If yes → resolve plan rule → rate check by API key.</li>
 * <li>If no → apply IP-based default rate limit.</li>
 * <li>Attempt Redis distributed check first; fall back to in-memory.</li>
 * <li>If blocked → return 429 with Retry-After and rate-limit headers.</li>
 * </ol>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RuleConfigService ruleConfigService;
    private final RateLimiterFactory rateLimiterFactory;
    private final IpRateLimitService ipRateLimitService;
    private final RedisRateLimitStore redisStore;
    private final MetricsService metricsService;

    public RateLimitInterceptor(RuleConfigService ruleConfigService,
            RateLimiterFactory rateLimiterFactory,
            IpRateLimitService ipRateLimitService,
            RedisRateLimitStore redisStore,
            MetricsService metricsService) {
        this.ruleConfigService = ruleConfigService;
        this.rateLimiterFactory = rateLimiterFactory;
        this.ipRateLimitService = ipRateLimitService;
        this.redisStore = redisStore;
        this.metricsService = metricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        ApiKeyInfo apiKeyInfo = (ApiKeyInfo) request.getAttribute(ApiKeyAuthFilter.API_KEY_ATTR);
        RateLimitResult result;
        String limiterKey;

        if (apiKeyInfo != null) {
            // Authenticated request — use plan-based limiting
            limiterKey = apiKeyInfo.getApiKey();
            RateLimitRule rule = apiKeyInfo.hasCustomRule()
                    ? apiKeyInfo.getCustomRule()
                    : ruleConfigService.getRuleForPlan(apiKeyInfo.getPlanType());
            AlgorithmType algorithm = ruleConfigService.getActiveAlgorithm();

            // Try Redis first
            result = redisStore.check(limiterKey, rule, algorithm);
            if (result == null) {
                // Fallback to in-memory
                RateLimiter limiter = rateLimiterFactory.getLimiter(algorithm);
                result = limiter.check(limiterKey, rule);
            }
        } else {
            // Anonymous request — IP-based limiting
            limiterKey = getClientIp(request);
            result = ipRateLimitService.check(limiterKey);
        }

        // Record metrics
        metricsService.recordRequest(limiterKey, result.isAllowed());

        // Set rate-limit headers on all responses
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));

        if (!result.isAllowed()) {
            return rejectRequest(response, result);
        }

        return true; // allow the request to proceed
    }

    private boolean rejectRequest(HttpServletResponse response, RateLimitResult result) throws IOException {
        long retryAfterSec = result.getRetryAfterSeconds();
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSec));
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded. Please retry after " + retryAfterSec + " seconds.");
        body.put("retryAfterSeconds", retryAfterSec);

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
