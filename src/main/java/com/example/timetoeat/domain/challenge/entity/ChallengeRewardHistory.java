package com.example.timetoeat.domain.challenge.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "challenge_reward_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reward_member_type_achieved",
                columnNames = {"member_id", "type", "achieved_at"}
        )
)
@Entity
public class ChallengeRewardHistory extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // reward 대상 회원
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    // WEEK / MONTH
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private RewardType type;

    // 달성 기준 일자 (주간: 보상 가능해진 ‘오늘’, 월간: 그 달의 말일)
    @Column(name = "achieved_at", nullable = false)
    private LocalDate achievedAt;

    // 수령 시각(KST)
    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Builder
    private ChallengeRewardHistory(MemberEntity member, RewardType type,
                                   LocalDate achievedAt, LocalDateTime claimedAt) {
        this.member = member;
        this.type = type;
        this.achievedAt = achievedAt;
        this.claimedAt = claimedAt;
    }

    public enum RewardType { WEEK, MONTH }
}
