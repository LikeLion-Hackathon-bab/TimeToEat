package com.example.timetoeat.global.auth.demo;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.member.repository.SocialAccountRepository;
import com.example.timetoeat.domain.preference.repository.MemberFoodPreferenceRepository;
import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoLoginUnitTest {
    static final String SECRET = "s".repeat(40);
    static final Instant NOW = Instant.parse("2026-12-01T00:00:00Z");

    MemberJpaRepository members;
    SocialAccountRepository socials;
    MemberFoodPreferenceRepository prefs;
    JwtService jwt;

    DemoLoginService service(String until) {
        var props = new DemoLoginProperties(SECRET, until, Clock.fixed(NOW, ZoneOffset.UTC));
        return new DemoLoginService(props, members, socials, prefs, jwt);
    }

    MockMvc mvc(DemoLoginService svc, DemoLoginRateLimiter limiter) {
        return MockMvcBuilders.standaloneSetup(new DemoLoginController(svc, limiter)).build();
    }

    static String body(String secret, int account) {
        return "{\"secret\":\"" + secret + "\",\"account\":" + account + "}";
    }

    @BeforeEach
    void setUp() {
        members = mock(MemberJpaRepository.class);
        socials = mock(SocialAccountRepository.class);
        prefs = mock(MemberFoodPreferenceRepository.class);
        jwt = mock(JwtService.class);
        for (int n = 1; n <= 5; n++) {
            long id = 100 + n;
            when(members.findByEmail(DemoAccounts.email(n))).thenReturn(Optional.of(
                    MemberEntity.builder().id(id).username(DemoAccounts.nickname(n)).email(DemoAccounts.email(n)).role(Role.ROLE_MEMBER).build()));
        }
        when(jwt.issueForMember(anyLong())).thenAnswer(i -> TokenDto.builder()
                .accessToken("access-" + i.getArgument(0)).refreshToken("refresh-" + i.getArgument(0))
                .refreshTokenMaxAge(604_800_000).build());
    }

    @Test void enablingWithShortOrMissingSecretFailsStartup() {
        assertThrows(IllegalStateException.class, () -> new DemoLoginProperties("short", "", Clock.systemUTC()));
        assertThrows(IllegalStateException.class, () -> new DemoLoginProperties(null, "", Clock.systemUTC()));
        assertThrows(IllegalStateException.class, () -> new DemoLoginProperties(SECRET, "tomorrow", Clock.systemUTC()));
    }

    @Test void untilIsHonoured() {
        assertFalse(service("2026-12-01T09:00:01+09:00").expired());
        assertTrue(service("2026-12-01T09:00:00+09:00").expired());
        assertFalse(service("").expired());
    }

    @Test void namesAreRealisticAndEmailsAreHidden() {
        assertEquals("김민준", DemoAccounts.nickname(1));
        assertEquals("이소율", DemoAccounts.nickname(5));
        assertEquals("qa_demo3@users.noreply.babmuckdang", DemoAccounts.email(3));
        assertFalse(DemoAccounts.valid(0));
        assertFalse(DemoAccounts.valid(6));
        assertFalse(DemoAccounts.valid(null));
    }

    @Test void wrongSecretIs403AndIssuesNothing() throws Exception {
        mvc(service(""), new DemoLoginRateLimiter()).perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON).content(body("x".repeat(40), 1)))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Set-Cookie"));
        mvc(service(""), new DemoLoginRateLimiter()).perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(jwt, never()).issueForMember(anyLong());
    }

    @Test void accountOutOfRangeIs400() throws Exception {
        mvc(service(""), new DemoLoginRateLimiter()).perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON).content(body(SECRET, 6)))
                .andExpect(status().isBadRequest());
        verify(jwt, never()).issueForMember(anyLong());
    }

    @Test void expiredIs404() throws Exception {
        mvc(service("2026-11-30T00:00:00Z"), new DemoLoginRateLimiter()).perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON).content(body(SECRET, 1)))
                .andExpect(status().isNotFound());
        verify(jwt, never()).issueForMember(anyLong());
    }

    @Test void successSetsRefreshCookieAndReturnsAccessToken() throws Exception {
        when(prefs.existsByMember_Id(102L)).thenReturn(false);
        mvc(service(""), new DemoLoginRateLimiter()).perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON).content(body(SECRET, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("access-102"))
                .andExpect(jsonPath("$.data.signupRequired").value(true))
                .andExpect(header().string("X-Access-Token", "access-102"))
                .andExpect(header().stringValues("Set-Cookie",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.startsWith("refreshToken=refresh-102"),
                                org.hamcrest.Matchers.containsString("HttpOnly")))));
    }

    @Test void onboardedDemoMemberSkipsOnboarding() {
        when(prefs.existsByMember_Id(101L)).thenReturn(true);
        var r = service("").login(SECRET, 1);
        assertEquals(DemoLoginService.Outcome.OK, r.outcome());
        assertFalse(r.signupRequired());
    }

    @Test void memberWithSocialAccountIsNeverTargeted() {
        when(socials.existsByMemberEntity_Id(103L)).thenReturn(true);
        assertEquals(DemoLoginService.Outcome.NOT_DEMO_MEMBER, service("").login(SECRET, 3).outcome());
        when(members.findByEmail(DemoAccounts.email(4))).thenReturn(Optional.empty());
        assertEquals(DemoLoginService.Outcome.NOT_DEMO_MEMBER, service("").login(SECRET, 4).outcome());
        verify(jwt, never()).issueForMember(anyLong());
    }

    @Test void rateLimitPerIpPerMinute() throws Exception {
        var clock = new MutableClock();
        var limiter = new DemoLoginRateLimiter(clock);
        for (int i = 0; i < DemoLoginRateLimiter.LIMIT_PER_MINUTE; i++) assertTrue(limiter.tryAcquire("1.1.1.1"));
        assertFalse(limiter.tryAcquire("1.1.1.1"));
        assertTrue(limiter.tryAcquire("2.2.2.2"));
        clock.now += 60_001;
        assertTrue(limiter.tryAcquire("1.1.1.1"));

        var full = new DemoLoginRateLimiter(clock);
        for (int i = 0; i < DemoLoginRateLimiter.LIMIT_PER_MINUTE; i++) full.tryAcquire("127.0.0.1");
        mvc(service(""), full).perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON).content(body(SECRET, 1)))
                .andExpect(status().isTooManyRequests());
    }

    static class MutableClock extends Clock {
        long now = 1_000_000;
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
        @Override public long millis() { return now; }
    }
}
