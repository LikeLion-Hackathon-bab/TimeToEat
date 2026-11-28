package com.example.timetoeat.qa.defects;

import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import com.example.timetoeat.domain.challenge.exception.ChallengeErrorCode;
import com.example.timetoeat.domain.challenge.repository.ChallengeRewardHistoryRepository;
import com.example.timetoeat.domain.challenge.service.ChallengeQueryService;
import com.example.timetoeat.domain.challenge.service.ChallengeRewardCommandService;
import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.friend.service.FriendMealQueryService;
import com.example.timetoeat.domain.meal.entity.MemberMealStatus;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
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

    /**
     * T3: 7일 연속을 넘겨 계속 인증하면 WEEK 보상을 매일 다시 받을 수 있었다(achievedAt=오늘). → 연속 7일 한 주기에 1회.
     * 저장소 대역은 저장된 기록(유형·달성일)으로 exists 질의에 답한다.
     */
    private final List<ChallengeRewardHistory> history = new ArrayList<>();
    private final CouponCommandService rewardCoupons = mock(CouponCommandService.class);

    private ChallengeRewardCommandService rewardServiceOn(LocalDate day) {
        return rewardServiceOn(day, true);
    }

    private ChallengeRewardCommandService rewardServiceOn(LocalDate day, boolean weekAvailable) {
        var status = mock(ChallengeQueryService.class);
        when(status.getMyStatus(1L)).thenReturn(ChallengeStatusResponse.of(Collections.nCopies(7, true), 7, 8, 30, weekAvailable, false));
        var repo = mock(ChallengeRewardHistoryRepository.class);
        when(repo.existsByMember_IdAndTypeAndAchievedAt(anyLong(), any(), any())).thenAnswer(i -> history.stream()
                .anyMatch(h -> h.getType() == i.getArgument(1) && h.getAchievedAt().equals(i.getArgument(2))));
        when(repo.existsByMember_IdAndTypeAndAchievedAtBetween(anyLong(), any(), any(), any())).thenAnswer(i -> history.stream()
                .anyMatch(h -> h.getType() == i.getArgument(1)
                        && !h.getAchievedAt().isBefore(i.getArgument(2)) && !h.getAchievedAt().isAfter(i.getArgument(3))));
        when(repo.save(any())).thenAnswer(i -> { history.add(i.getArgument(0)); return i.getArgument(0); });
        var members = mock(MemberJpaRepository.class);
        when(members.findById(1L)).thenReturn(Optional.of(MemberEntity.builder().id(1L).build()));
        return new ChallengeRewardCommandService(status, repo, members, rewardCoupons,
                Clock.fixed(day.atTime(20, 0).atZone(KST).toInstant(), KST));
    }

    @Test
    void weekRewardIsNotClaimableAgainTheNextDayOfTheSameStreak() {
        LocalDate day7 = LocalDate.of(2026, 11, 7);
        rewardServiceOn(day7).claim(1L, RewardType.WEEK);
        var e = assertThrows(CustomException.class, () -> rewardServiceOn(day7.plusDays(1)).claim(1L, RewardType.WEEK),
                "같은 연속 기록(8일째)으로 두 번째 주간 보상");
        assertEquals(ChallengeErrorCode.ALREADY_CLAIMED, e.getBaseError());
        verify(rewardCoupons, times(1)).issueRewardCoupon(1L, "WEEK");
    }

    @Test
    void fourteenDayStreakGivesTwoWeekRewardsNotEight() {
        LocalDate day7 = LocalDate.of(2026, 11, 7);
        int claimed = 0;
        for (int d = 0; d <= 7; d++) {   // 7일째~14일째 매일 시도
            try { rewardServiceOn(day7.plusDays(d)).claim(1L, RewardType.WEEK); claimed++; }
            catch (CustomException e) { assertEquals(ChallengeErrorCode.ALREADY_CLAIMED, e.getBaseError()); }
        }
        assertEquals(2, claimed, "7일째·14일째만");
        assertEquals(List.of(day7, day7.plusDays(7)), history.stream().map(ChallengeRewardHistory::getAchievedAt).toList());
    }

    /** 상태 조회가 이미 받은 주기에 weekRewardAvailable=false 를 줄 때도 수령 재요청은 400(불가)이 아니라 409(이미 받음). */
    @Test
    void claimAfterStatusHidesButtonIsStillConflict() {
        LocalDate day8 = LocalDate.of(2026, 11, 8);
        history.add(ChallengeRewardHistory.builder().type(RewardType.WEEK).achievedAt(day8.minusDays(1)).build());
        var svc = rewardServiceOn(day8, false);
        var e = assertThrows(CustomException.class, () -> svc.claim(1L, RewardType.WEEK));
        assertEquals(ChallengeErrorCode.ALREADY_CLAIMED, e.getBaseError());
        verifyNoInteractions(rewardCoupons);
    }

    @Test
    void monthRewardIsUnaffectedByWeekWindow() {
        LocalDate lastDay = LocalDate.of(2026, 11, 30);
        var status = mock(ChallengeQueryService.class);
        when(status.getMyStatus(1L)).thenReturn(ChallengeStatusResponse.of(Collections.nCopies(7, true), 7, 30, 30, true, true));
        var repo = mock(ChallengeRewardHistoryRepository.class);
        var members = mock(MemberJpaRepository.class);
        when(members.findById(1L)).thenReturn(Optional.of(MemberEntity.builder().id(1L).build()));
        var svc = new ChallengeRewardCommandService(status, repo, members, rewardCoupons,
                Clock.fixed(lastDay.atTime(20, 0).atZone(KST).toInstant(), KST));
        svc.claim(1L, RewardType.MONTH);
        verify(repo, never()).existsByMember_IdAndTypeAndAchievedAtBetween(anyLong(), eq(RewardType.MONTH), any(), any());
        verify(rewardCoupons).issueRewardCoupon(1L, "MONTH");
    }

    /** T4: 방금(1시간 미만 전) 수동으로 '배고파요'로 바꾼 친구가 "0시간 공복이에요"로 보였다. */
    private com.example.timetoeat.domain.friend.dto.response.FriendMealItemResponse manualHungry(Duration ago) {
        var friendDir = mock(com.example.timetoeat.domain.friend.service.FriendDirectory.class);
        var meals = mock(MemberMealStatusRepository.class);
        var now = LocalDateTime.of(2026, 11, 28, 12, 0);
        var m = MemberEntity.builder().id(2L).username("가").build();
        when(friendDir.friendsOf(1L)).thenReturn(List.of(m));
        when(meals.findByMember_IdIn(any())).thenReturn(List.of(MemberMealStatus.builder().member(m).manualFastingSince(now.minus(ago)).build()));
        return new FriendMealQueryService(Clock.fixed(now.atZone(KST).toInstant(), KST),
                friendDir, meals, false)
                .getDemoList(1L, "ALL").get(0);
    }

    @Test
    void justSwitchedToHungryDoesNotShowZeroHours() {
        var item = manualHungry(Duration.ofMinutes(5));
        assertNotEquals("0시간 공복이에요", item.getLabel());
        assertEquals("공복이에요", item.getLabel());
        assertTrue(item.isHungry());
        assertEquals("공복이에요", manualHungry(Duration.ofMinutes(59)).getLabel());
        assertEquals("1시간 공복이에요", manualHungry(Duration.ofMinutes(60)).getLabel(), "1시간부터는 시간 표시");
    }
}
