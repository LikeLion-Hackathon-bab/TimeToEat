package com.example.timetoeat.global.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {
    @Test void defaultsAreKeptWhenNothingIsAdded() {
        assertEquals(SecurityConfig.DEFAULT_ORIGINS, SecurityConfig.allowedOrigins(null));
        assertEquals(SecurityConfig.DEFAULT_ORIGINS, SecurityConfig.allowedOrigins(""));
    }

    @Test void extraOriginsAreAppendedTrimmedAndDeduplicated() {
        var list = SecurityConfig.allowedOrigins(" https://bmd-qa.vercel.app/ ,https://1-2-3-4.sslip.io,https://babmuckdang.site, ,https://*.vercel.app");
        assertTrue(list.containsAll(SecurityConfig.DEFAULT_ORIGINS));
        assertTrue(list.contains("https://bmd-qa.vercel.app"));
        assertTrue(list.contains("https://1-2-3-4.sslip.io"));
        assertFalse(list.stream().anyMatch(o -> o.contains("*")), "credentials=true 와 와일드카드는 같이 쓰지 않는다");
        assertEquals(SecurityConfig.DEFAULT_ORIGINS.size() + 2, list.size());
    }
}
