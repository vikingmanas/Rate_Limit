package com.ratelimiter.model;

public class ApiKeyInfo {

    private String apiKey;
    private String owner;
    private PlanType planType;
    private RateLimitRule customRule; // null means use plan default

    public ApiKeyInfo() {}

    public ApiKeyInfo(String apiKey, String owner, PlanType planType) {
        this.apiKey = apiKey;
        this.owner = owner;
        this.planType = planType;
    }

    public ApiKeyInfo(String apiKey, String owner, PlanType planType, RateLimitRule customRule) {
        this.apiKey = apiKey;
        this.owner = owner;
        this.planType = planType;
        this.customRule = customRule;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public PlanType getPlanType() { return planType; }
    public void setPlanType(PlanType planType) { this.planType = planType; }

    public RateLimitRule getCustomRule() { return customRule; }
    public void setCustomRule(RateLimitRule customRule) { this.customRule = customRule; }

    public boolean hasCustomRule() {
        return customRule != null;
    }
}
