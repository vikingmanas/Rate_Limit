package com.ratelimiter.monitoring;

import com.ratelimiter.admin.RuleConfigService;
import com.ratelimiter.redis.RedisRateLimitStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes metrics REST endpoints and serves the monitoring dashboard page.
 */
@Controller
public class MetricsController {

    private final MetricsService metricsService;
    private final RuleConfigService ruleConfigService;
    private final RedisRateLimitStore redisStore;

    public MetricsController(MetricsService metricsService,
            RuleConfigService ruleConfigService,
            RedisRateLimitStore redisStore) {
        this.metricsService = metricsService;
        this.ruleConfigService = ruleConfigService;
        this.redisStore = redisStore;
    }

    /** Serve the dashboard page. */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    /** Overall metrics summary. */
    @GetMapping("/api/metrics/summary")
    @ResponseBody
    public Map<String, Object> summary() {
        Map<String, Object> data = new LinkedHashMap<>(metricsService.getSummary());
        data.put("activeAlgorithm", ruleConfigService.getActiveAlgorithm().name());
        data.put("redisAvailable", redisStore.isRedisAvailable());
        return data;
    }

    /** Top N users by request volume. */
    @GetMapping("/api/metrics/top-users")
    @ResponseBody
    public List<Map<String, Object>> topUsers() {
        return metricsService.getTopUsers(10);
    }

    /** Per-minute traffic timeline. */
    @GetMapping("/api/metrics/timeline")
    @ResponseBody
    public List<Map<String, Object>> timeline() {
        return metricsService.getTimeline();
    }
}
