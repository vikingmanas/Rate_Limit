package com.ratelimiter.admin;

import com.ratelimiter.auth.ApiKeyStore;
import com.ratelimiter.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin REST API for managing rate limit rules and API keys at runtime.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RuleConfigService ruleConfigService;
    private final ApiKeyStore apiKeyStore;

    public AdminController(RuleConfigService ruleConfigService, ApiKeyStore apiKeyStore) {
        this.ruleConfigService = ruleConfigService;
        this.apiKeyStore = apiKeyStore;
    }

    // ─── Rules Management ─────────────────────────────────────────

    @GetMapping("/rules")
    public ResponseEntity<Map<PlanType, RateLimitRule>> getAllRules() {
        return ResponseEntity.ok(ruleConfigService.getAllRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> updateRule(@RequestParam PlanType planType,
            @RequestBody RateLimitRule rule) {
        ruleConfigService.updateRule(planType, rule);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Rule updated for plan: " + planType);
        response.put("rule", rule);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{planType}")
    public ResponseEntity<Map<String, String>> deleteRule(@PathVariable PlanType planType) {
        boolean removed = ruleConfigService.deleteRule(planType);
        Map<String, String> response = new LinkedHashMap<>();
        if (removed) {
            response.put("message", "Rule deleted for plan: " + planType);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "No rule found for plan: " + planType);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── Algorithm Switching ──────────────────────────────────────

    @GetMapping("/config/algorithm")
    public ResponseEntity<Map<String, String>> getAlgorithm() {
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("activeAlgorithm", ruleConfigService.getActiveAlgorithm().name());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/config/algorithm")
    public ResponseEntity<Map<String, String>> setAlgorithm(@RequestParam AlgorithmType algorithm) {
        ruleConfigService.setActiveAlgorithm(algorithm);
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("message", "Algorithm switched to: " + algorithm);
        resp.put("activeAlgorithm", algorithm.name());
        return ResponseEntity.ok(resp);
    }

    // ─── API Key Management ──────────────────────────────────────

    @GetMapping("/keys")
    public ResponseEntity<Collection<ApiKeyInfo>> getAllKeys() {
        return ResponseEntity.ok(apiKeyStore.getAllKeys());
    }

    @PostMapping("/keys")
    public ResponseEntity<ApiKeyInfo> createKey(@RequestParam String owner,
            @RequestParam PlanType plan) {
        ApiKeyInfo key = apiKeyStore.createKey(owner, plan);
        return ResponseEntity.ok(key);
    }

    @DeleteMapping("/keys/{apiKey}")
    public ResponseEntity<Map<String, String>> deleteKey(@PathVariable String apiKey) {
        boolean removed = apiKeyStore.removeKey(apiKey);
        Map<String, String> response = new LinkedHashMap<>();
        if (removed) {
            response.put("message", "API key deleted: " + apiKey);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Key not found: " + apiKey);
            return ResponseEntity.notFound().build();
        }
    }
}
