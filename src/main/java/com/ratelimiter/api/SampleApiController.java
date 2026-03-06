package com.ratelimiter.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sample API endpoints to demonstrate rate limiting in action.
 * These are protected by the
 * {@link com.ratelimiter.interceptor.RateLimitInterceptor}.
 */
@RestController
@RequestMapping("/api")
public class SampleApiController {

    @GetMapping("/data")
    public Map<String, Object> getData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "Hello! This is protected data.");
        data.put("timestamp", Instant.now().toString());
        data.put("info", "If you see this, your request was within the rate limit.");
        return data;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "Distributed Rate Limiter");
        health.put("timestamp", Instant.now().toString());
        return health;
    }

    @GetMapping("/resource")
    public Map<String, Object> getResource() {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("id", (int) (Math.random() * 10000));
        resource.put("name", "Sample Resource");
        resource.put("description", "This endpoint demonstrates rate-limited resource access.");
        resource.put("timestamp", Instant.now().toString());
        return resource;
    }
}
