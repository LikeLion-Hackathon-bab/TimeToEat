package com.example.timetoeat.domain.referral.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "referral_code",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_referral_code_code", columnNames = "code")
        })
@Entity
public class ReferralCode extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추천인(초대한 사람)
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private MemberEntity inviter;

    // 공유되는 추천 코드 (쿼리파라미터로 사용)
    @Column(name = "code", nullable = false, length = 16, unique = true)
    private String code;

    // 만료시각(KST)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 수령자(가입 완료 후 추천 코드 등록한 사람)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "used_by_id")
    private MemberEntity usedBy;

    // 수령 시각
    private LocalDateTime usedAt;

    @Builder
    private ReferralCode(MemberEntity inviter, String code, LocalDateTime expiresAt) {
        this.inviter = inviter;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) { return now.isAfter(expiresAt); }
    public boolean isUsed() { return usedBy != null; }

    public void markUsed(MemberEntity receiver, LocalDateTime when) {
        this.usedBy = receiver;
        this.usedAt = when;
    }
}
