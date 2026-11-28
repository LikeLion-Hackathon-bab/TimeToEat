package com.example.timetoeat.qa.defects;

import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.entity.ReferralCode;
import com.example.timetoeat.domain.referral.exception.ReferralErrorCode;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.domain.referral.service.ReferralCommandService;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * spring-tests 작업(qa/spring-tests @ bba2549)에서 찾은 결함·정책 의심의 재현 시험. 처음에는 QA_DEFECTS=1 일 때만 돌았고,
 * 수정과 함께 상시 회귀 시험으로 옮겼다. 근거: qa-cloud/spring-tests/REPORT.md
 * T1(쿠폰 id 중복)은 쿠폰이 DB로 옮겨져 qa/CouponPersistenceIT 로 갔다.
 */
class SpringServiceDefectReproTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * T2: 한 회원이 서로 다른 추천인의 코드를 여러 개 등록해 등록마다 INVITE 쿠폰을 받았다 → 회원당 1회.
     * (동시 요청은 uk_referral_code_used_by 가 막는다: qa/ReferralRedeemOnceIT)
     */
    @Test
    void oneMemberCanRedeemOnlyOneReferralCode() {
        var codes = mock(ReferralCodeRepository.class);
        var members = mock(MemberJpaRepository.class);
        var coupons = mock(CouponCommandService.class);
        var now = LocalDateTime.of(2026, 11, 28, 12, 0);
        var svc = new ReferralCommandService(codes, members, coupons, Clock.fixed(now.atZone(KST).toInstant(), KST));
        var a = MemberEntity.builder().id(1L).build(); var b = MemberEntity.builder().id(3L).build();
        var me = MemberEntity.builder().id(2L).build();
        var codeA = ReferralCode.builder().inviter(a).code("CODEA00000").expiresAt(now.plusDays(1)).build();
        var codeB = ReferralCode.builder().inviter(b).code("CODEB00000").expiresAt(now.plusDays(1)).build();
        when(members.findById(2L)).thenReturn(Optional.of(me));
        when(codes.findForUpdateByCode("CODEA00000")).thenReturn(Optional.of(codeA));
        when(codes.findForUpdateByCode("CODEB00000")).thenReturn(Optional.of(codeB));
        // 저장소 대역: 등록 여부는 실제 행 상태(used_by)로 답한다
        when(codes.existsByUsedBy_Id(anyLong())).thenAnswer(i -> List.of(codeA, codeB).stream()
                .anyMatch(c -> c.getUsedBy() != null && c.getUsedBy().getId().equals(i.getArgument(0))));

        svc.redeem(2L, "CODEA00000");
        var e = assertThrows(CustomException.class, () -> svc.redeem(2L, "CODEB00000"), "두 번째 추천 코드 등록은 거절돼야 함");
        assertEquals(ReferralErrorCode.ALREADY_REDEEMED, e.getBaseError());
        assertNull(codeB.getUsedBy(), "거절된 코드는 사용 처리되지 않음");
        verify(coupons, times(1)).issueRewardCoupon(2L, "INVITE");
    }

    /** T2: 동시 요청이 사전 검사를 함께 통과해도 유니크 충돌이 나면 409, 쿠폰 없음. */
    @Test
    void concurrentSecondRedeemHittingUniqueConstraintIsRejectedWithoutCoupon() {
        var codes = mock(ReferralCodeRepository.class);
        var members = mock(MemberJpaRepository.class);
        var coupons = mock(CouponCommandService.class);
        var now = LocalDateTime.of(2026, 11, 28, 12, 0);
        var svc = new ReferralCommandService(codes, members, coupons, Clock.fixed(now.atZone(KST).toInstant(), KST));
        when(members.findById(2L)).thenReturn(Optional.of(MemberEntity.builder().id(2L).build()));
        when(codes.findForUpdateByCode("CODEB00000")).thenReturn(Optional.of(ReferralCode.builder()
                .inviter(MemberEntity.builder().id(3L).build()).code("CODEB00000").expiresAt(now.plusDays(1)).build()));
        when(codes.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_referral_code_used_by"));

        var e = assertThrows(CustomException.class, () -> svc.redeem(2L, "CODEB00000"));
        assertEquals(ReferralErrorCode.ALREADY_REDEEMED, e.getBaseError());
        verifyNoInteractions(coupons);
    }
}
