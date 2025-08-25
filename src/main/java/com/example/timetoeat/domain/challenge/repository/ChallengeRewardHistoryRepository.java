package com.example.timetoeat.domain.challenge.repository;

import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory;
import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ChallengeRewardHistoryRepository extends JpaRepository<ChallengeRewardHistory, Long> {

    boolean existsByMember_IdAndTypeAndAchievedAt(Long memberId, RewardType type, LocalDate achievedAt);
}
