package com.example.timetoeat.global.auth.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * 시연 로그인 설정. DEMO_LOGIN_ENABLED=true 일 때만 빈이 생긴다(기본 꺼짐 → 경로 404).
 * 켰는데 비밀값이 32자 미만이거나 만료 시각 형식이 틀리면 기동을 멈춘다(잘못 켠 채로 운영되지 않게).
 */
@Component
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoLoginProperties {
    static final int MIN_SECRET_LENGTH = 32;
    public static final int ACCOUNT_COUNT = 5;

    private final byte[] secret;
    private final Instant until;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DemoLoginProperties(@Value("${demo-login.secret:}") String secret,
                               @Value("${demo-login.until:}") String until) {
        this(secret, until, Clock.systemUTC());
    }

    DemoLoginProperties(String secret, String until, Clock clock) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("DEMO_LOGIN_ENABLED=true 이면 DEMO_LOGIN_SECRET(" + MIN_SECRET_LENGTH + "자 이상)이 필요합니다.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.until = parse(until);
        this.clock = clock;
    }

    private static Instant parse(String until) {
        if (until == null || until.isBlank()) return null;
        try {
            return OffsetDateTime.parse(until.trim()).toInstant(); // 예: 2026-12-05T23:59:00+09:00
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalStateException("DEMO_LOGIN_UNTIL은 ISO-8601 시각(예 2026-12-05T23:59:00+09:00)이어야 합니다: " + until, e);
        }
    }

    byte[] secret() { return secret.clone(); }

    /** DEMO_LOGIN_UNTIL 이 지났으면 꺼진 것으로 본다. */
    boolean expired() {
        return until != null && !clock.instant().isBefore(until);
    }

    Instant until() { return until; }

    Clock clock() { return clock; }
}
