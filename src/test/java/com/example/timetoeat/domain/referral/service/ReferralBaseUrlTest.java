package com.example.timetoeat.domain.referral.service;

import org.junit.jupiter.api.Test;

import static com.example.timetoeat.domain.referral.service.ReferralCommandService.resolveBaseUrl;
import static org.junit.jupiter.api.Assertions.*;

/** 재리뷰 M: 추천 링크 기본값이 만료 도메인(babmuckdang.site)이 아니라 Client 주소 환경변수에서 온다. */
class ReferralBaseUrlTest {
    @Test void explicitPropertyWins() {
        assertEquals("http://127.0.0.1:3100/invite", resolveBaseUrl("http://127.0.0.1:3100/invite/", "https://a.vercel.app", "https://b.vercel.app/"));
    }

    @Test void clientBaseUrlThenLoginRedirectOrigin() {
        assertEquals("https://a.vercel.app/invite", resolveBaseUrl("", "https://a.vercel.app/", "https://b.vercel.app/"));
        assertEquals("https://b.vercel.app/invite", resolveBaseUrl(null, "", "https://b.vercel.app/after-login?x=1"));
    }

    @Test void nothingConfiguredGivesARelativeLinkNotTheExpiredDomain() {
        assertEquals("/invite", resolveBaseUrl("", "", ""));
        assertEquals("/invite", resolveBaseUrl(null, null, "not a url"));
        assertFalse(resolveBaseUrl(null, null, null).contains("babmuckdang.site"));
    }
}
