package com.example.timetoeat.qa.defects;

import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import com.example.timetoeat.domain.challenge.repository.ChallengeRewardHistoryRepository;
import com.example.timetoeat.domain.challenge.service.ChallengeQueryService;
import com.example.timetoeat.domain.challenge.service.ChallengeRewardCommandService;
import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.coupon.service.CouponQueryService;
import com.example.timetoeat.domain.friend.service.FriendMealQueryService;
import com.example.timetoeat.domain.meal.entity.MemberMealStatus;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.entity.ReferralCode;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.domain.referral.service.ReferralCommandService;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * spring-tests 작업에서 찾은 결함·정책 의심의 재현 시험. 기대 동작을 단언하므로 현재 main 코드에서는 실패한다.
 * 기본 빌드를 깨지 않도록 QA_DEFECTS=1 일 때만 돈다: `QA_DEFECTS=1 ./gradlew test --tests '*SpringServiceDefectReproTest'`.
 * 수정이 들어가면 해당 시험의 조건을 지우고 일반 시험으로 옮긴다. 근거: qa-cloud/spring-tests/REPORT.md
 */
@EnabledIfEnvironmentVariable(named = "QA_DEFECTS", matches = "1")
class SpringServiceDefectReproTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * T1(결함): 보상 쿠폰 id = System.currentTimeMillis(). 같은 밀리초에 두 장이 발급되면(예: 챌린지 WEEK 와 MONTH 를 말일에 연달아,
     * 또는 동시 요청) id가 같아져, 한 장을 쓰면 같은 id의 다른 쿠폰도 '사용 완료'로 바뀌고 남은 한 장은 409로 쓸 수 없다.
     * CouponCommandService.java:34, CouponQueryService.java:52-54(usedBox 가 id 로만 구분).
     */
    @Test
    void rewardCouponsIssuedBackToBackGetDistinctIds() {
        var query = new CouponQueryService();
        var command = new CouponCommandService(query);
        for (int i = 0; i < 200; i++) command.issueRewardCoupon(1L, i % 2 == 0 ? "WEEK" : "INVITE");
        var ids = query.getMyCoupons(1L, null).stream().filter(c -> c.getTitle().endsWith("리워드"))
                .map(CouponResponse::getCouponId).toList();
        assertEquals(200, ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size(), "보상 쿠폰 id 중복: 한 장 사용 시 다른 장도 사용 처리됨");
    }

    @Test
    void usingOneRewardDoesNotConsumeAnotherIssuedInTheSameMillisecond() {
        var query = new CouponQueryService();
        var command = new CouponCommandService(query);
        long id = 0;
        // 같은 밀리초에 두 장이 나올 때까지 시도(보통 첫 회)
        for (int attempt = 0; attempt < 1000; attempt++) {
            var q = new CouponQueryService(); var c = new CouponCommandService(q);
            c.issueRewardCoupon(1L, "WEEK"); c.issueRewardCoupon(1L, "MONTH");
            var r = q.getMyCoupons(1L, CouponStatus.UNUSED).stream().filter(x -> x.getTitle().endsWith("리워드")).toList();
            if (r.get(0).getCouponId().equals(r.get(1).getCouponId())) {
                c.use(1L, r.get(0).getCouponId());
                long stillUnused = q.getMyCoupons(1L, CouponStatus.UNUSED).stream().filter(x -> x.getTitle().endsWith("리워드")).count();
                assertEquals(1, stillUnused, "한 장만 썼는데 두 장 모두 사용 완료가 됨");
                return;
            }
        }
        // 같은 밀리초가 한 번도 안 나오면 이 환경에서는 재현 불가(통과 처리하지 않고 알림)
        fail("1000회 시도 동안 같은 밀리초 발급이 없어 재현 못 함");
    }

    /**
     * T2(정책 의심): 한 회원이 서로 다른 추천인의 코드를 여러 개 등록할 수 있고, 등록마다 INVITE 쿠폰이 나온다.
     * 추천 코드 등록은 계정당 1회가 일반적(쿠폰 무한 수령 방지). ReferralCommandService.java:93-112 에 '이미 등록한 회원' 검사 없음.
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
        when(members.findById(2L)).thenReturn(Optional.of(me));
        when(codes.findForUpdateByCode("CODEA00000")).thenReturn(Optional.of(ReferralCode.builder().inviter(a).code("CODEA00000").expiresAt(now.plusDays(1)).build()));
        when(codes.findForUpdateByCode("CODEB00000")).thenReturn(Optional.of(ReferralCode.builder().inviter(b).code("CODEB00000").expiresAt(now.plusDays(1)).build()));

        svc.redeem(2L, "CODEA00000");
        assertThrows(CustomException.class, () -> svc.redeem(2L, "CODEB00000"), "두 번째 추천 코드 등록은 거절돼야 함");
        verify(coupons, times(1)).issueRewardCoupon(2L, "INVITE");
    }

    /**
     * T3(정책 의심): 7일 연속을 넘겨 계속 인증하면 WEEK 보상을 매일 다시 받을 수 있다(achievedAt=오늘).
     * 14일 연속이면 8장. 시안은 '7일' 한 주기 보상. ChallengeRewardCommandService.java:42-44.
     */
    @Test
    void weekRewardIsNotClaimableAgainTheNextDayOfTheSameStreak() {
        var status = mock(ChallengeQueryService.class);
        when(status.getMyStatus(1L)).thenReturn(ChallengeStatusResponse.of(Collections.nCopies(7, true), 7, 8, 30, true, false));
        var history = new HashSet<String>();
        var repo = mock(ChallengeRewardHistoryRepository.class);
        when(repo.existsByMember_IdAndTypeAndAchievedAt(anyLong(), any(), any()))
                .thenAnswer(i -> history.contains(i.getArgument(1) + ":" + i.getArgument(2)));
        when(repo.save(any())).thenAnswer(i -> { var h = (com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory) i.getArgument(0);
            history.add(h.getType() + ":" + h.getAchievedAt()); return h; });
        var members = mock(MemberJpaRepository.class);
        when(members.findById(1L)).thenReturn(Optional.of(MemberEntity.builder().id(1L).build()));
        var coupons = mock(CouponCommandService.class);

        LocalDate day7 = LocalDate.of(2026, 11, 7);
        new ChallengeRewardCommandService(status, repo, members, coupons, Clock.fixed(day7.atTime(20, 0).atZone(KST).toInstant(), KST))
                .claim(1L, RewardType.WEEK);
        var nextDay = new ChallengeRewardCommandService(status, repo, members, coupons,
                Clock.fixed(day7.plusDays(1).atTime(20, 0).atZone(KST).toInstant(), KST));
        assertThrows(CustomException.class, () -> nextDay.claim(1L, RewardType.WEEK), "같은 연속 기록(8일째)으로 두 번째 주간 보상");
        verify(coupons, times(1)).issueRewardCoupon(1L, "WEEK");
    }

    /**
     * T4(화면 문구): 방금 수동으로 '배고파요'로 바꾼 친구가 "0시간 공복이에요"로 보인다. 1시간 미만은 다른 문구가 자연스럽다.
     * FriendMealQueryService.java:73-74(마지막 식사 기준도 같은 계산이지만 그쪽은 4시간 창 이후라 0이 안 나온다).
     */
    @Test
    void justSwitchedToHungryDoesNotShowZeroHours() {
        var members = mock(MemberJpaRepository.class);
        var meals = mock(MemberMealStatusRepository.class);
        var now = LocalDateTime.of(2026, 11, 28, 12, 0);
        var m = MemberEntity.builder().id(2L).username("가").build();
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(1L)).thenReturn(List.of(m));
        when(meals.findByMember_IdIn(any())).thenReturn(List.of(MemberMealStatus.builder().member(m).manualFastingSince(now.minusMinutes(5)).build()));
        var item = new FriendMealQueryService(Clock.fixed(now.atZone(KST).toInstant(), KST), members, meals)
                .getDemoList(1L, "ALL").get(0);
        assertNotEquals("0시간 공복이에요", item.getLabel());
    }
}
