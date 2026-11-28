package com.example.timetoeat.global.auth.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** IP별 1분 슬라이딩 창 제한(서버 1대 기준 메모리). 실패·성공 모두 센다. */
@Component
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoLoginRateLimiter {
    static final int LIMIT_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED_IPS = 10_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DemoLoginRateLimiter() { this(Clock.systemUTC()); }

    DemoLoginRateLimiter(Clock clock) { this.clock = clock; }

    public boolean tryAcquire(String ip) {
        long now = clock.millis();
        if (hits.size() > MAX_TRACKED_IPS) hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) { return e.getValue().isEmpty() || e.getValue().peekLast() <= now - WINDOW_MS; }
        });
        Deque<Long> q = hits.computeIfAbsent(ip == null ? "unknown" : ip, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() <= now - WINDOW_MS) q.pollFirst();
            if (q.size() >= LIMIT_PER_MINUTE) return false;
            q.addLast(now);
            return true;
        }
    }
}
