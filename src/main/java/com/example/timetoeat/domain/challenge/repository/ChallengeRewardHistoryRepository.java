package com.example.timetoeat.domain.challenge.repository;

import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ChallengeRewardHistoryRepository extends JpaRepository<ChallengeRewardHistory, Long> {

    boolean existsByMember_IdAndTypeAndAchievedAt(Long memberId, RewardType type, LocalDate achievedAt);

    /** 달성일이 [from, to] 안인 보상 기록이 있는지(주간 보상: 한 연속 주기 7일에 1회) */
    boolean existsByMember_IdAndTypeAndAchievedAtBetween(Long memberId, RewardType type, LocalDate from, LocalDate to);
}
