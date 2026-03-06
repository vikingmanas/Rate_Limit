package com.ratelimiter.monitoring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe metrics collection for rate limiter activity.
 * Uses {@link LongAdder} for high-throughput counter updates.
 */
@Service
public class MetricsService {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder allowedRequests = new LongAdder();
    private final LongAdder blockedRequests = new LongAdder();

    /** Per-key request counts. */
    private final ConcurrentHashMap<String, LongAdder> perKeyRequests = new ConcurrentHashMap<>();
    /** Per-key blocked counts. */
    private final ConcurrentHashMap<String, LongAdder> perKeyBlocked = new ConcurrentHashMap<>();

    /** Per-minute traffic log (last 60 entries). */
    private final LinkedHashMap<String, long[]> minuteTraffic = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, long[]> eldest) {
            return size() > 60;
        }
    };

    /** Current minute counters for the timeline. */
    private final LongAdder currentMinuteAllowed = new LongAdder();
    private final LongAdder currentMinuteBlocked = new LongAdder();
    private volatile String currentMinuteKey = minuteKey();

    public void recordRequest(String key, boolean allowed) {
        totalRequests.increment();
        if (allowed) {
            allowedRequests.increment();
            currentMinuteAllowed.increment();
        } else {
            blockedRequests.increment();
            currentMinuteBlocked.increment();
        }

        perKeyRequests.computeIfAbsent(key, k -> new LongAdder()).increment();
        if (!allowed) {
            perKeyBlocked.computeIfAbsent(key, k -> new LongAdder()).increment();
        }
    }

    /** Flush minute counters every 60 seconds. */
    @Scheduled(fixedRate = 60000)
    public void flushMinute() {
        String key = currentMinuteKey;
        long allowed = currentMinuteAllowed.sumThenReset();
        long blocked = currentMinuteBlocked.sumThenReset();

        synchronized (minuteTraffic) {
            minuteTraffic.put(key, new long[] { allowed, blocked });
        }
        currentMinuteKey = minuteKey();
    }

    // ─── Accessors ────────────────────────────────────────────────

    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getAllowedRequests() {
        return allowedRequests.sum();
    }

    public long getBlockedRequests() {
        return blockedRequests.sum();
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRequests", totalRequests.sum());
        summary.put("allowedRequests", allowedRequests.sum());
        summary.put("blockedRequests", blockedRequests.sum());
        double blockRate = totalRequests.sum() == 0 ? 0 : (double) blockedRequests.sum() / totalRequests.sum() * 100;
        summary.put("blockRatePercent", Math.round(blockRate * 100.0) / 100.0);
        return summary;
    }

    /** Top N keys by total request count, descending. */
    public List<Map<String, Object>> getTopUsers(int n) {
        List<Map<String, Object>> users = new ArrayList<>();
        perKeyRequests.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .limit(n)
                .forEach(entry -> {
                    Map<String, Object> user = new LinkedHashMap<>();
                    user.put("key", entry.getKey());
                    user.put("requests", entry.getValue().sum());
                    LongAdder blocked = perKeyBlocked.get(entry.getKey());
                    user.put("blocked", blocked != null ? blocked.sum() : 0);
                    users.add(user);
                });
        return users;
    }

    /** Returns the per-minute traffic timeline (last 60 minutes). */
    public List<Map<String, Object>> getTimeline() {
        List<Map<String, Object>> timeline = new ArrayList<>();
        synchronized (minuteTraffic) {
            minuteTraffic.forEach((minute, counts) -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("minute", minute);
                entry.put("allowed", counts[0]);
                entry.put("blocked", counts[1]);
                timeline.add(entry);
            });
        }
        // Add current ongoing minute
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("minute", currentMinuteKey);
        current.put("allowed", currentMinuteAllowed.sum());
        current.put("blocked", currentMinuteBlocked.sum());
        timeline.add(current);

        return timeline;
    }

    private static String minuteKey() {
        return Instant.now().toString().substring(0, 16); // e.g. "2026-03-06T13:08"
    }
}
