package com.example.timetoeat.domain.referral.service;

import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.entity.ReferralCode;
import com.example.timetoeat.domain.referral.exception.ReferralErrorCode;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.global.error.BaseError;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** F85 추천인 코드 발급·등록·조회. 만료·사용됨·자기 추천 거절, 코드 정규화, 충돌 재시도, 링크 형식. */
class ReferralCodeFlowTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);

    private ReferralCodeRepository codes;
    private MemberJpaRepository members;
    private CouponCommandService coupons;
    private ReferralCommandService command;
    private final MemberEntity inviter = MemberEntity.builder().id(1L).username("초대한사람").build();
    private final MemberEntity invitee = MemberEntity.builder().id(2L).username("새친구").build();

    @BeforeEach
    void setUp() {
        codes = mock(ReferralCodeRepository.class);
        members = mock(MemberJpaRepository.class);
        coupons = mock(CouponCommandService.class);
        command = new ReferralCommandService(codes, members, coupons, e -> {}, Clock.fixed(NOW.atZone(KST).toInstant(), KST));
        ReflectionTestUtils.setField(command, "ttlDays", 7);
        ReflectionTestUtils.setField(command, "configuredBaseUrl", "");
        ReflectionTestUtils.setField(command, "clientBaseUrl", "https://babmuckdang.vercel.app/");
        ReflectionTestUtils.setField(command, "clientLoginRedirect", "");
        when(members.findById(1L)).thenReturn(Optional.of(inviter));
        when(members.findById(2L)).thenReturn(Optional.of(invitee));
        when(codes.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ReferralCode code(String c, MemberEntity owner, LocalDateTime expiresAt) {
        return ReferralCode.builder().inviter(owner).code(c).expiresAt(expiresAt).build();
    }

    private static BaseError error(Runnable r) {
        return assertThrows(CustomException.class, r::run).getBaseError();
    }

    @Test
    void createdCodeIsTenUnambiguousCharsWithLinkAndTtl() {
        var res = command.create(1L);
        assertTrue(res.getCode().matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{10}"), res.getCode());
        assertEquals("https://babmuckdang.vercel.app/invite?code=" + res.getCode(), res.getLink());
        assertEquals(NOW.plusDays(7), res.getExpiresAt());
    }

    @Test
    void createSkipsCodesThatAlreadyExist() {
        when(codes.existsByCode(anyString())).thenReturn(true, true, false);
        command.create(1L);
        verify(codes, times(3)).existsByCode(anyString());
        verify(codes, times(1)).save(any());
    }

    @Test
    void createRetriesWhenUniqueConstraintRaces() {
        when(codes.save(any())).thenThrow(new DataIntegrityViolationException("uk_referral_code_code"))
                .thenAnswer(i -> i.getArgument(0));
        assertNotNull(command.create(1L).getCode());
        verify(codes, times(2)).save(any());
    }

    @Test
    void createForUnknownMemberIsNotFound() {
        assertEquals(MemberErrorCode.MEMBER_NOT_FOUND, error(() -> command.create(99L)));
        verify(codes, never()).save(any());
    }

    @Test
    void redeemNormalizesCaseAndSpacesThenMarksUsedAndIssuesInviteCoupon() {
        var c = code("ABCDEFGH23", inviter, NOW.plusDays(1));
        when(codes.findForUpdateByCode("ABCDEFGH23")).thenReturn(Optional.of(c));
        command.redeem(2L, "  abcdefgh23 ");
        assertTrue(c.isUsed());
        assertSame(invitee, c.getUsedBy());
        assertEquals(NOW, c.getUsedAt());
        verify(coupons).issueRewardCoupon(2L, "INVITE");
    }

    @Test
    void codeExpiringExactlyNowIsStillAcceptedOneSecondLaterIsNot() {
        var edge = code("EDGE000000", inviter, NOW);
        when(codes.findForUpdateByCode("EDGE000000")).thenReturn(Optional.of(edge));
        command.redeem(2L, "EDGE000000");
        assertTrue(edge.isUsed());

        var past = code("PAST000000", inviter, NOW.minusSeconds(1));
        when(codes.findForUpdateByCode("PAST000000")).thenReturn(Optional.of(past));
        assertEquals(ReferralErrorCode.CODE_EXPIRED, error(() -> command.redeem(2L, "PAST000000")));
        assertFalse(past.isUsed());
    }

    @Test
    void usedCodeIsConflict() {
        var c = code("USED000000", inviter, NOW.plusDays(1));
        c.markUsed(MemberEntity.builder().id(3L).build(), NOW.minusHours(1));
        when(codes.findForUpdateByCode("USED000000")).thenReturn(Optional.of(c));
        assertEquals(ReferralErrorCode.CODE_ALREADY_USED, error(() -> command.redeem(2L, "USED000000")));
        assertEquals(3L, c.getUsedBy().getId(), "처음 사용자 유지");
        verifyNoInteractions(coupons);
    }

    @Test
    void selfReferralIsRejected() {
        var c = code("SELF000000", inviter, NOW.plusDays(1));
        when(codes.findForUpdateByCode("SELF000000")).thenReturn(Optional.of(c));
        assertEquals(ReferralErrorCode.SELF_REFERRAL_NOT_ALLOWED, error(() -> command.redeem(1L, "SELF000000")));
        assertFalse(c.isUsed());
        verifyNoInteractions(coupons);
    }

    @Test
    void unknownOrNullCodeIsNotFound() {
        when(codes.findForUpdateByCode(any())).thenReturn(Optional.empty());
        assertEquals(ReferralErrorCode.CODE_NOT_FOUND, error(() -> command.redeem(2L, "NOPE")));
        assertEquals(ReferralErrorCode.CODE_NOT_FOUND, error(() -> command.redeem(2L, null)));
        verifyNoInteractions(coupons);
    }

    @Test
    void errorCodesMapToHttpStatus() {
        assertEquals(404, ReferralErrorCode.CODE_NOT_FOUND.getHttpStatus().value());
        assertEquals(400, ReferralErrorCode.CODE_EXPIRED.getHttpStatus().value());
        assertEquals(409, ReferralErrorCode.CODE_ALREADY_USED.getHttpStatus().value());
        assertEquals(400, ReferralErrorCode.SELF_REFERRAL_NOT_ALLOWED.getHttpStatus().value());
    }

    @Test
    void myReferralsShowUsageAndRequireAKnownMember() {
        var used = code("USED000000", inviter, NOW.plusDays(1));
        used.markUsed(invitee, NOW);
        var fresh = code("FRESH00000", inviter, NOW.plusDays(7));
        when(codes.findAllByInviter_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(fresh, used));
        var query = new ReferralQueryService(codes, members);

        var list = query.getMyReferrals(1L);
        assertEquals(List.of("FRESH00000", "USED000000"), list.stream().map(r -> r.getCode()).toList());
        assertFalse(list.get(0).isUsed());
        assertNull(list.get(0).getUsedByMemberId());
        assertTrue(list.get(1).isUsed());
        assertEquals(2L, list.get(1).getUsedByMemberId());
        assertEquals("새친구", list.get(1).getUsedByUserName());
        assertEquals(MemberErrorCode.MEMBER_NOT_FOUND, error(() -> query.getMyReferrals(99L)));
    }
}
