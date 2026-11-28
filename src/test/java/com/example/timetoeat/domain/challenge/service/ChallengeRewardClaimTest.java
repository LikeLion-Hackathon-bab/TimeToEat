package com.example.timetoeat.domain.challenge.service;

import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import com.example.timetoeat.domain.challenge.exception.ChallengeErrorCode;
import com.example.timetoeat.domain.challenge.repository.ChallengeRewardHistoryRepository;
import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.global.error.BaseError;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** F75 챌린지 쿠폰 받기: 가능 여부, 중복 수령(사전 조회·유니크 충돌), 달성일 기록, 쿠폰 발급 순서. */
class ChallengeRewardClaimTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 21, 30);

    private ChallengeQueryService statusService;
    private ChallengeRewardHistoryRepository rewards;
    private MemberJpaRepository members;
    private CouponCommandService coupons;
    private ChallengeRewardCommandService service;

    @BeforeEach
    void setUp() {
        statusService = mock(ChallengeQueryService.class);
        rewards = mock(ChallengeRewardHistoryRepository.class);
        members = mock(MemberJpaRepository.class);
        coupons = mock(CouponCommandService.class);
        service = new ChallengeRewardCommandService(statusService, rewards, members, coupons,
                Clock.fixed(NOW.atZone(KST).toInstant(), KST));
        when(members.findById(1L)).thenReturn(Optional.of(MemberEntity.builder().id(1L).username("나").build()));
        when(rewards.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void available(boolean week, boolean month) {
        when(statusService.getMyStatus(1L)).thenReturn(ChallengeStatusResponse.of(
                List.of(true, true, true, true, true, true, true), 7, 18, 30, week, month));
    }

    private static BaseError code(Runnable r) {
        return assertThrows(CustomException.class, r::run).getBaseError();
    }

    @Test
    void weekRewardRecordsTodayAndIssuesWeekCoupon() {
        available(true, false);
        service.claim(1L, RewardType.WEEK);
        var saved = ArgumentCaptor.forClass(ChallengeRewardHistory.class);
        verify(rewards).save(saved.capture());
        assertEquals(LocalDate.of(2026, 11, 18), saved.getValue().getAchievedAt());
        assertEquals(NOW, saved.getValue().getClaimedAt());
        assertEquals(RewardType.WEEK, saved.getValue().getType());
        verify(coupons).issueRewardCoupon(1L, "WEEK");
    }

    @Test
    void monthRewardRecordsLastDayOfMonth() {
        available(false, true);
        service.claim(1L, RewardType.MONTH);
        verify(rewards).existsByMember_IdAndTypeAndAchievedAt(1L, RewardType.MONTH, LocalDate.of(2026, 11, 30));
        verify(coupons).issueRewardCoupon(1L, "MONTH");
    }

    @Test
    void notAvailableIsRejectedBeforeAnyWrite() {
        available(false, false);
        assertEquals(ChallengeErrorCode.REWARD_NOT_AVAILABLE, code(() -> service.claim(1L, RewardType.WEEK)));
        assertEquals(ChallengeErrorCode.REWARD_NOT_AVAILABLE, code(() -> service.claim(1L, RewardType.MONTH)));
        verify(rewards, never()).save(any());
        verifyNoInteractions(coupons);
    }

    @Test
    void weekAvailabilityDoesNotUnlockMonth() {
        available(true, false);
        assertEquals(ChallengeErrorCode.REWARD_NOT_AVAILABLE, code(() -> service.claim(1L, RewardType.MONTH)));
    }

    @Test
    void alreadyClaimedIsConflictAndNoSecondCoupon() {
        available(true, false);
        when(rewards.existsByMember_IdAndTypeAndAchievedAt(1L, RewardType.WEEK, NOW.toLocalDate())).thenReturn(true);
        assertEquals(ChallengeErrorCode.ALREADY_CLAIMED, code(() -> service.claim(1L, RewardType.WEEK)));
        verify(rewards, never()).save(any());
        verifyNoInteractions(coupons);
    }

    @Test
    void uniqueViolationFromDoubleClickIsConflictAndNoCoupon() {
        available(true, false);
        when(rewards.save(any())).thenThrow(new DataIntegrityViolationException("uk_reward_member_type_achieved"));
        assertEquals(ChallengeErrorCode.ALREADY_CLAIMED, code(() -> service.claim(1L, RewardType.WEEK)));
        verifyNoInteractions(coupons);
    }

    @Test
    void unknownMemberIsNotFound() {
        when(statusService.getMyStatus(2L)).thenReturn(ChallengeStatusResponse.of(List.of(), 7, 0, 30, true, false));
        assertEquals(MemberErrorCode.MEMBER_NOT_FOUND, code(() -> service.claim(2L, RewardType.WEEK)));
        verifyNoInteractions(coupons);
    }

    @Test
    void errorCodesMapToExpectedHttpStatus() {
        assertEquals(400, ChallengeErrorCode.REWARD_NOT_AVAILABLE.getHttpStatus().value());
        assertEquals(409, ChallengeErrorCode.ALREADY_CLAIMED.getHttpStatus().value());
        assertEquals(400, ChallengeErrorCode.INVALID_REWARD_TYPE.getHttpStatus().value());
    }
}
