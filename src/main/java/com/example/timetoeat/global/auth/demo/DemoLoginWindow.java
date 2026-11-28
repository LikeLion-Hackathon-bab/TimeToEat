package com.example.timetoeat.global.auth.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeParseException;

/**
 * 시연 로그인이 지금 열려 있는지(켜짐 && 기한 전). 시연 기능이 꺼져 있어도 항상 등록돼
 * refresh 재발급·기동 시 토큰 정리가 이 판단을 쓴다.
 * <ul>
 *   <li>DEMO_LOGIN_UNTIL: 오프셋이 있으면 그대로, 없으면 Asia/Seoul 시각으로 읽는다.</li>
 *   <li>기한은 기동 시점부터 최대 {@value #MAX_DAYS}일. 비어 있으면 {@value #DEFAULT_DAYS}일.</li>
 *   <li>형식이 틀리면 서버는 그대로 뜨고 시연만 닫는다(fail-closed, API 전체 장애를 만들지 않음).</li>
 * </ul>
 */
@Slf4j
@Component
public class DemoLoginWindow {
    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    static final int DEFAULT_DAYS = 7;
    static final int MAX_DAYS = 14;

    private final boolean enabled;
    private final Instant until; // enabled 일 때만 의미. null 이면 닫힘(형식 오류)
    private final Clock clock;

    @Autowired
    public DemoLoginWindow(@Value("${demo-login.enabled:false}") boolean enabled,
                           @Value("${demo-login.until:}") String until) {
        this(enabled, until, Clock.systemUTC());
    }

    DemoLoginWindow(boolean enabled, String until, Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
        this.until = enabled ? resolve(until, clock.instant()) : null;
        if (enabled && this.until != null) {
            log.warn("시연 로그인 기한: {} (Asia/Seoul)", this.until.atZone(SEOUL).toLocalDateTime());
        }
    }

    static Instant resolve(String raw, Instant startedAt) {
        Instant cap = startedAt.plus(Duration.ofDays(MAX_DAYS));
        if (raw == null || raw.isBlank()) {
            return startedAt.plus(Duration.ofDays(DEFAULT_DAYS));
        }
        Instant parsed;
        try {
            parsed = OffsetDateTime.parse(raw.trim()).toInstant();
        } catch (DateTimeParseException withOffset) {
            try {
                parsed = LocalDateTime.parse(raw.trim()).atZone(SEOUL).toInstant();
            } catch (DateTimeParseException e) {
                log.error("DEMO_LOGIN_UNTIL 형식 오류로 시연 로그인을 닫습니다(예 2026-12-05T23:59): {}", raw);
                return null;
            }
        }
        if (parsed.isAfter(cap)) {
            log.warn("DEMO_LOGIN_UNTIL이 {}일 상한을 넘어 {}로 줄입니다.", MAX_DAYS, cap);
            return cap;
        }
        return parsed;
    }

    /** 켜져 있고 기한 전이면 true. */
    public boolean isOpen() {
        return enabled && until != null && clock.instant().isBefore(until);
    }

    Instant until() { return until; }
}
