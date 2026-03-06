package com.ratelimiter.model;

public class RateLimitRule {

    private int maxRequests;
    private int windowSeconds;
    private double burstMultiplier;
    private AlgorithmType algorithmType;

    public RateLimitRule() {
        this.maxRequests = 30;
        this.windowSeconds = 60;
        this.burstMultiplier = 1.0;
        this.algorithmType = AlgorithmType.TOKEN_BUCKET;
    }

    public RateLimitRule(int maxRequests, int windowSeconds, double burstMultiplier, AlgorithmType algorithmType) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.burstMultiplier = burstMultiplier;
        this.algorithmType = algorithmType;
    }

    public int getMaxRequests() { return maxRequests; }
    public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

    public double getBurstMultiplier() { return burstMultiplier; }
    public void setBurstMultiplier(double burstMultiplier) { this.burstMultiplier = burstMultiplier; }

    public AlgorithmType getAlgorithmType() { return algorithmType; }
    public void setAlgorithmType(AlgorithmType algorithmType) { this.algorithmType = algorithmType; }

    /** Effective max tokens for burst-aware algorithms (e.g. Token Bucket). */
    public int getBurstCapacity() {
        return (int) Math.ceil(maxRequests * burstMultiplier);
    }
}
