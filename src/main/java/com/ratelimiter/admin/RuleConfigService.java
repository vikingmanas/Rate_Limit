package com.ratelimiter.admin;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.PlanType;
import com.ratelimiter.model.RateLimitRule;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the live rate-limit rules per plan tier.
 * Supports hot-reload — changes take effect immediately.
 */
@Service
public class RuleConfigService {

    private final ConcurrentHashMap<PlanType, RateLimitRule> rules = new ConcurrentHashMap<>();
    private volatile AlgorithmType activeAlgorithm = AlgorithmType.TOKEN_BUCKET;

    @PostConstruct
    public void init() {
        // Default plan rules (matching application.yml)
        rules.put(PlanType.FREE, new RateLimitRule(30, 60, 1.0, AlgorithmType.TOKEN_BUCKET));
        rules.put(PlanType.PREMIUM, new RateLimitRule(200, 60, 2.0, AlgorithmType.TOKEN_BUCKET));
        rules.put(PlanType.ADMIN, new RateLimitRule(1000, 60, 3.0, AlgorithmType.TOKEN_BUCKET));
    }

    public RateLimitRule getRuleForPlan(PlanType plan) {
        return rules.getOrDefault(plan, rules.get(PlanType.FREE));
    }

    public void updateRule(PlanType plan, RateLimitRule rule) {
        rules.put(plan, rule);
    }

    public boolean deleteRule(PlanType plan) {
        return rules.remove(plan) != null;
    }

    public Map<PlanType, RateLimitRule> getAllRules() {
        return Map.copyOf(rules);
    }

    public AlgorithmType getActiveAlgorithm() {
        return activeAlgorithm;
    }

    public void setActiveAlgorithm(AlgorithmType algorithmType) {
        this.activeAlgorithm = algorithmType;
    }
}
