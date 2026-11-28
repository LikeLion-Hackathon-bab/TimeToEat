package com.example.timetoeat.global.auth.jwt;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.entity.RefreshToken;
import com.example.timetoeat.domain.member.repository.RefreshTokenRepository;
import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.auth.service.MemberService;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** 갱신 응답이 브라우저에 닿지 못해 쿠키에 이전 refresh 토큰이 남은 경우(새로고침과 갱신이 겹침). */
class JwtServiceReissueTest {
    private JwtProvider provider;
    private RefreshTokenRepository repo;
    private MemberService members;
    private RefreshToken row;

    @BeforeEach
    void setUp() {
        provider = new JwtProvider("0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(provider, "accessTockenExpiration", 3_600_000);
        ReflectionTestUtils.setField(provider, "refreshTockenExpiration", 604_800_000);
        repo = mock(RefreshTokenRepository.class);
        members = mock(MemberService.class);
        MemberEntity member = MemberEntity.builder().id(2L).username("b").email("b@test").role(Role.ROLE_MEMBER).build();
        when(members.getById(2L)).thenReturn(member);
        row = RefreshToken.create(2L, provider.generateRefreshToken(member, new Date(System.currentTimeMillis() - 5_000)),
                new Date(System.currentTimeMillis() + 604_800_000));
        when(repo.findByMemberIdForUpdate(anyLong())).thenReturn(Optional.of(row));
    }

    private JwtService service(long graceMs) {
        return new JwtService(provider, repo, members, new RefreshRotationGrace(graceMs));
    }

    @Test
    void previousTokenIsAcceptedOnceWithinGrace() {
        JwtService svc = service(30_000);
        String r0 = row.getToken();
        String r1 = svc.reissue(r0).getRefreshToken(); // 응답 유실: 브라우저는 r1을 받지 못함
        assertNotEquals(r0, r1);
        String r2 = svc.reissue(r0).getRefreshToken(); // 새로고침 후 쿠키의 r0로 재시도
        assertEquals(r2, row.getToken());
    }

    @Test
    void previousTokenIsRejectedWithoutGrace() {
        JwtService svc = service(0);
        String r0 = row.getToken();
        svc.reissue(r0);
        assertThrows(CustomException.class, () -> svc.reissue(r0));
    }

    @Test
    void unknownTokenIsRejected() {
        JwtService svc = service(30_000);
        MemberEntity other = MemberEntity.builder().id(2L).username("b").email("b@test").role(Role.ROLE_MEMBER).build();
        String forged = provider.generateRefreshToken(other, new Date(System.currentTimeMillis() - 60_000));
        assertThrows(CustomException.class, () -> svc.reissue(forged));
    }
}
