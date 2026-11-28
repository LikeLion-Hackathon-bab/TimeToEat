package com.example.timetoeat.global.auth.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.*;

/** N4: 소셜 로그인 실패는 JSON 401 대신 Client 로그인 화면으로 302. */
class OAuth2LoginFailureHandlerTest {
    private final OAuth2LoginFailureHandler handler =
            new OAuth2LoginFailureHandler("https://babmuckdang.vercel.app/");

    private MockHttpServletResponse fail(Exception e) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), res,
                (org.springframework.security.core.AuthenticationException) e);
        return res;
    }

    @Test void userCancelRedirectsToLoginWithReason() throws Exception {
        MockHttpServletResponse res = fail(new OAuth2AuthenticationException(new OAuth2Error("access_denied")));
        assertEquals(302, res.getStatus());
        assertEquals("https://babmuckdang.vercel.app/login?error=social_login_failed&reason=access_denied",
                res.getRedirectedUrl());
        assertEquals("", res.getContentAsString());
    }

    @Test void missingStateRedirectsToLogin() throws Exception {
        MockHttpServletResponse res = fail(new OAuth2AuthenticationException(
                new OAuth2Error("authorization_request_not_found")));
        assertEquals("https://babmuckdang.vercel.app/login?error=social_login_failed&reason=authorization_request_not_found",
                res.getRedirectedUrl());
    }

    @Test void unsafeOrUnknownReasonIsDropped() throws Exception {
        assertEquals("https://babmuckdang.vercel.app/login?error=social_login_failed",
                fail(new OAuth2AuthenticationException(new OAuth2Error("x\"><script>"))).getRedirectedUrl());
        assertEquals("https://babmuckdang.vercel.app/login?error=social_login_failed",
                fail(new BadCredentialsException("boom")).getRedirectedUrl());
    }

    @Test void unsetOrRelativeBaseUsesSameSiteRelativeLogin() {
        assertEquals("/login?error=social_login_failed&reason=access_denied",
                new OAuth2LoginFailureHandler("").failureUrl("access_denied"));
        assertEquals("/login?error=social_login_failed", new OAuth2LoginFailureHandler(null).failureUrl(null));
        assertEquals("/login?error=social_login_failed", new OAuth2LoginFailureHandler("/").failureUrl(null));
    }

    @Test void baseWithPathOrQueryStillGoesToLogin() {
        var h = new OAuth2LoginFailureHandler("https://example.com/app/?from=x#top");
        assertEquals("https://example.com/login?error=social_login_failed", h.failureUrl(null));
    }
}
