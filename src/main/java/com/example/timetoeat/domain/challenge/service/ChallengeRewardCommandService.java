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
            throw new CustomException(ChallengeErrorCode.REWARD_NOT_AVAILABLE);
        }

        // 2) 멤버 로드
        MemberEntity me = memberRepo.findById(meId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 3) 중복 수령 체크 (+ 레이스 대비)
        if (rewardRepo.existsByMember_IdAndTypeAndAchievedAt(meId, type, achievedAt)) {
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
