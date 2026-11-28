package com.example.timetoeat.global.auth.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 방금 회전된 refresh 토큰을 짧게 기억한다.
 * 갱신 응답(Set-Cookie)이 새로고침·네트워크 끊김으로 브라우저에 닿지 못하면 쿠키에는 이전 토큰이 남는다.
 * 유예 시간 안에 그 토큰이 다시 오면 한 번 더 회전해 준다. 단일 인스턴스 메모리 저장이며 스키마 변경이 없다.
 */
@Component
public class RefreshRotationGrace {
    private final long graceMillis;
    private final Map<String, Entry> rotated = new ConcurrentHashMap<>();

    private record Entry(Long memberId, long validUntil) {}

    public RefreshRotationGrace(@Value("${jwt.refresh-reuse-grace-ms:30000}") long graceMillis) {
        this.graceMillis = graceMillis;
    }

    public void remember(Long memberId, String previousToken, long nowMillis) {
        if (graceMillis <= 0 || previousToken == null) return;
        rotated.values().removeIf(e -> e.validUntil() < nowMillis);
        rotated.put(previousToken, new Entry(memberId, nowMillis + graceMillis));
    }

    public boolean isRecentlyRotated(Long memberId, String token, long nowMillis) {
        Entry e = rotated.get(token);
        return e != null && e.memberId().equals(memberId) && nowMillis <= e.validUntil();
    }

    public void forget(Long memberId) {
        rotated.values().removeIf(e -> e.memberId().equals(memberId));
    }
}
