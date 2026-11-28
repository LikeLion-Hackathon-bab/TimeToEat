package com.example.timetoeat.global.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {
    @Test void defaultsAreKeptWhenNothingIsAdded() {
        assertEquals(SecurityConfig.DEFAULT_ORIGINS, SecurityConfig.allowedOrigins(null));
        assertEquals(SecurityConfig.DEFAULT_ORIGINS, SecurityConfig.allowedOrigins(""));
    }

    @Test void extraOriginsAreAppendedTrimmedAndDeduplicated() {
        var list = SecurityConfig.allowedOrigins(" https://bmd-qa.vercel.app/ ,https://1-2-3-4.sslip.io,https://bab-muk-dang-client.vercel.app, ,https://*.vercel.app");
        assertTrue(list.containsAll(SecurityConfig.DEFAULT_ORIGINS));
        assertTrue(list.contains("https://bmd-qa.vercel.app"));
        assertTrue(list.contains("https://1-2-3-4.sslip.io"));
        assertFalse(list.stream().anyMatch(o -> o.contains("*")), "credentials=true 와 와일드카드는 같이 쓰지 않는다");
        assertEquals(SecurityConfig.DEFAULT_ORIGINS.size() + 2, list.size());
    }

    /** 보안 점검 M2: 기본 목록에는 운영 Client 출처(https)만. localhost·IP·만료 도메인 없음. */
    @Test void defaultHasOnlyHttpsProductionOrigins() {
        for (String o : SecurityConfig.DEFAULT_ORIGINS) {
            assertTrue(o.startsWith("https://"), o);
            assertFalse(o.contains("localhost") || o.contains("127.0.0.1") || o.matches("https://\\d+\\.\\d+\\.\\d+\\.\\d+.*")
                    || o.contains("babmuckdang.site"), o);
        }
        assertTrue(SecurityConfig.allowedOrigins("http://localhost:3001").contains("http://localhost:3001"), "개발 출처는 환경변수로");
    }
}
