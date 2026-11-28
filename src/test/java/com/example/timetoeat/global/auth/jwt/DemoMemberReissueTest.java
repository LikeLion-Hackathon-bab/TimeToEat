package com.example.timetoeat.global.auth.jwt;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.entity.RefreshToken;
import com.example.timetoeat.domain.member.repository.RefreshTokenRepository;
import com.example.timetoeat.global.auth.demo.DemoAccounts;
import com.example.timetoeat.global.auth.demo.DemoLoginWindow;
import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.auth.service.MemberService;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** 리뷰 D1: 시연이 닫히면 시연 회원의 refresh 재발급을 거절하고 토큰을 지운다. 일반 회원은 영향 없음. */
class DemoMemberReissueTest {
    private JwtService svc(String email, DemoLoginWindow window, RefreshTokenRepository repo) {
        JwtProvider provider = new JwtProvider("0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(provider, "accessTockenExpiration", 3_600_000);
        ReflectionTestUtils.setField(provider, "refreshTockenExpiration", 604_800_000);
        MemberService members = mock(MemberService.class);
        MemberEntity m = MemberEntity.builder().id(7L).username("김민준").email(email).role(Role.ROLE_MEMBER).build();
        when(members.getById(7L)).thenReturn(m);
        RefreshToken row = RefreshToken.create(7L, provider.generateRefreshToken(m, new Date()), new Date(System.currentTimeMillis() + 604_800_000));
        when(repo.findByMemberIdForUpdate(anyLong())).thenReturn(Optional.of(row));
        JwtService s = new JwtService(provider, repo, members, new RefreshRotationGrace(0));
        ReflectionTestUtils.setField(s, "demoWindow", window);
        return s;
    }

    private String token(JwtService s, RefreshTokenRepository repo) {
        return repo.findByMemberIdForUpdate(7L).orElseThrow().getToken();
    }

    @Test void demoMemberRejectedWhenDemoClosed() {
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        JwtService s = svc(DemoAccounts.email(1), new DemoLoginWindow(false, ""), repo);
        assertThrows(CustomException.class, () -> s.reissue(token(s, repo)));
        verify(repo).deleteAllByMemberId(7L);
    }

    @Test void demoMemberAllowedWhileOpen() {
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        JwtService s = svc(DemoAccounts.email(1), new DemoLoginWindow(true, ""), repo);
        assertNotNull(s.reissue(token(s, repo)).getAccessToken());
    }

    @Test void realMemberUnaffectedEvenWhenDemoClosed() {
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        JwtService s = svc("someone@kakao.test", null, repo);
        assertNotNull(s.reissue(token(s, repo)).getAccessToken());
        verify(repo, never()).deleteAllByMemberId(anyLong());
    }
}
