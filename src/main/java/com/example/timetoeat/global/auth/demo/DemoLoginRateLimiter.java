package com.example.timetoeat.global.auth.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1분 슬라이딩 창 제한(서버 1대 기준 메모리). 틀린 시연 코드만 센다(맞는 코드는 컨트롤러에서 건너뜀, 보안 재검토 N1).
 * Vercel·Caddy 뒤에서는 여러 시연자가 같은 출구 IP로 보일 수 있어(리뷰 D2) IP별 한도를 넉넉히 두고,
 * 전체 합계 한도를 따로 둔다.
 */
@Component
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoLoginRateLimiter {
    static final int LIMIT_PER_MINUTE = 30;
    static final int GLOBAL_LIMIT_PER_MINUTE = 120;
    private static final String GLOBAL = "\u0000global";
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
            if (GLOBAL.equals(e.getKey())) return false;
            synchronized (e.getValue()) { return e.getValue().isEmpty() || e.getValue().peekLast() <= now - WINDOW_MS; }
        });
        Deque<Long> all = hits.computeIfAbsent(GLOBAL, k -> new ArrayDeque<>());
        Deque<Long> q = hits.computeIfAbsent(ip == null ? "unknown" : ip, k -> new ArrayDeque<>());
        synchronized (all) {
            synchronized (q) {
                trim(all, now);
                trim(q, now);
                if (q.size() >= LIMIT_PER_MINUTE || all.size() >= GLOBAL_LIMIT_PER_MINUTE) return false;
                q.addLast(now);
                all.addLast(now);
                return true;
            }
        }
    }

    private static void trim(Deque<Long> q, long now) {
        while (!q.isEmpty() && q.peekFirst() <= now - WINDOW_MS) q.pollFirst();
    }
}
