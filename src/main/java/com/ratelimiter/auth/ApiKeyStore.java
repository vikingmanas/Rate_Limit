package com.ratelimiter.auth;

import com.ratelimiter.model.ApiKeyInfo;
import com.ratelimiter.model.PlanType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory API key store. Thread-safe via ConcurrentHashMap.
 * Pre-seeded with sample keys for testing.
 */
@Component
public class ApiKeyStore {

    private final ConcurrentHashMap<String, ApiKeyInfo> keys = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Pre-seed demo keys
        addKey(new ApiKeyInfo("free-key-1", "demo-free-user", PlanType.FREE));
        addKey(new ApiKeyInfo("free-key-2", "demo-free-user-2", PlanType.FREE));
        addKey(new ApiKeyInfo("premium-key-1", "demo-premium-user", PlanType.PREMIUM));
        addKey(new ApiKeyInfo("admin-key-1", "system-admin", PlanType.ADMIN));
    }

    public ApiKeyInfo validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return keys.get(apiKey);
    }

    public ApiKeyInfo addKey(ApiKeyInfo info) {
        keys.put(info.getApiKey(), info);
        return info;
    }

    public ApiKeyInfo createKey(String owner, PlanType plan) {
        String key = UUID.randomUUID().toString();
        ApiKeyInfo info = new ApiKeyInfo(key, owner, plan);
        keys.put(key, info);
        return info;
    }

    public boolean removeKey(String apiKey) {
        return keys.remove(apiKey) != null;
    }

    public Collection<ApiKeyInfo> getAllKeys() {
        return keys.values();
    }
}
