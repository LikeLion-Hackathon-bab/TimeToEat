package com.example.timetoeat.domain.challenge.service;

import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import com.example.timetoeat.domain.challenge.exception.ChallengeErrorCode;
import com.example.timetoeat.domain.challenge.repository.ChallengeRewardHistoryRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeRewardCommandService {

    private final ChallengeQueryService challengeQueryService;
    private final ChallengeRewardHistoryRepository rewardRepo;
    private final MemberJpaRepository memberRepo;
    private final CouponCommandService couponCommandService;

    @Qualifier("kstClock")
    private final Clock clock;

    public void claim(Long meId, RewardType type) {
        // 1) 현재 보상 가능 여부 확인
        ChallengeStatusResponse status = challengeQueryService.getMyStatus(meId);
        boolean ok;
        LocalDate achievedAt;

        LocalDate today = LocalDate.now(clock);
        switch (type) {
            case WEEK -> {
                ok = status.isWeekRewardAvailable();
                achievedAt = today; // ‘오늘’ 보상 가능해졌다고 간주
            }
            case MONTH -> {
                ok = status.isMonthRewardAvailable();
                achievedAt = YearMonth.from(today).atEndOfMonth(); // 그 달의 말일
            }
            default -> throw new CustomException(ChallengeErrorCode.INVALID_REWARD_TYPE);
        }
        if (!ok) {
            // 상태 조회는 이번 주기 보상을 이미 받았으면 weekRewardAvailable=false 를 준다 → 그 경우는 '불가'가 아니라 '이미 받음'(409)
            if (type == RewardType.WEEK && rewardRepo.existsByMember_IdAndTypeAndAchievedAtBetween(
                    meId, type, ChallengeQueryService.weekClaimWindowStart(achievedAt), achievedAt)) {
                throw new CustomException(ChallengeErrorCode.ALREADY_CLAIMED);
            }
            throw new CustomException(ChallengeErrorCode.REWARD_NOT_AVAILABLE);
        }

        // 2) 멤버 로드
        MemberEntity me = memberRepo.findById(meId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 3) 중복 수령 체크 (+ 레이스 대비)
        if (rewardRepo.existsByMember_IdAndTypeAndAchievedAt(meId, type, achievedAt)) {
            throw new CustomException(ChallengeErrorCode.ALREADY_CLAIMED);
        }
        // 주간 보상은 연속 7일 한 주기에 1회. 연속이 이어지는 8일째부터는 weekRewardAvailable 이 계속 true 라
        // 매일 받을 수 있었다(14일 연속이면 8장). 직전 수령 뒤 7일이 지나야 다시 받으므로 연속 N일에 최대 N/7장이고,
        // 연속이 끊기면 새 7일을 채우는 동안 이 창은 자연히 지난다.
        if (type == RewardType.WEEK
                && rewardRepo.existsByMember_IdAndTypeAndAchievedAtBetween(
                        meId, type, ChallengeQueryService.weekClaimWindowStart(achievedAt), achievedAt)) {
            throw new CustomException(ChallengeErrorCode.ALREADY_CLAIMED);
        }

        // 4) 기록 저장 (DB 유니크 제약으로 더블클릭 방어)
        try {
            rewardRepo.save(
                    ChallengeRewardHistory.builder()
                            .member(me)
                            .type(type)
                            .achievedAt(achievedAt)
                            .claimedAt(LocalDateTime.now(clock))
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 눌렀을 때 유니크 제약 위반 → 이미 수령으로 간주
            throw new CustomException(ChallengeErrorCode.ALREADY_CLAIMED);
        }

        // 5) 쿠폰 발급 (현재는 스텁; 실제 구현 시 쿠폰 엔티티/저장 필요)
        couponCommandService.issueRewardCoupon(meId, type.name());
    }
}
