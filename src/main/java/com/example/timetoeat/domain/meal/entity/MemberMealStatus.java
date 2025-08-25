package com.example.timetoeat.domain.meal.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_meal_status",
        uniqueConstraints = @UniqueConstraint(name = "uk_meal_member", columnNames = "member_id"))
@Entity
public class MemberMealStatus extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    // 마지막 식사 시각(수동 ON/게시글/태그 시 갱신)
    @Column(name = "last_meal_at")
    private LocalDateTime lastMealAt;

    // 수동 OFF 시각(있으면 이때부터 공복으로 간주)
    @Column(name = "manual_fasting_since")
    private LocalDateTime manualFastingSince;

    @Builder
    private MemberMealStatus(MemberEntity member, LocalDateTime lastMealAt, LocalDateTime manualFastingSince) {
        this.member = member;
        this.lastMealAt = lastMealAt;
        this.manualFastingSince = manualFastingSince;
    }

    public static MemberMealStatus of(MemberEntity member) {
        return MemberMealStatus.builder().member(member).build();
    }

    public void ate(LocalDateTime when) {
        this.lastMealAt = when;
        this.manualFastingSince = null; // 수동 OFF 해제
    }

    public void setManualFasting(LocalDateTime since) {
        this.manualFastingSince = since;
    }
}
