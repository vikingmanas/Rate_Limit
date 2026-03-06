package com.ratelimiter.model;

public class RateLimitResult {

    private final boolean allowed;
    private final int limit;
    private final int remaining;
    private final long retryAfterMillis;

    private RateLimitResult(boolean allowed, int limit, int remaining, long retryAfterMillis) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.retryAfterMillis = retryAfterMillis;
    }

    public static RateLimitResult allowed(int limit, int remaining) {
        return new RateLimitResult(true, limit, remaining, 0);
    }

    public static RateLimitResult blocked(int limit, long retryAfterMillis) {
        return new RateLimitResult(false, limit, 0, retryAfterMillis);
    }

    public boolean isAllowed() { return allowed; }
    public int getLimit() { return limit; }
    public int getRemaining() { return remaining; }
    public long getRetryAfterMillis() { return retryAfterMillis; }

    public long getRetryAfterSeconds() {
        return (retryAfterMillis + 999) / 1000; // ceil division
    }
}
