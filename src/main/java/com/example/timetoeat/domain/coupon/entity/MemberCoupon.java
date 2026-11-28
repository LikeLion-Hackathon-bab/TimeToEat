package com.example.timetoeat.domain.coupon.entity;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse.CouponType;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 회원 쿠폰함의 쿠폰 1장(F75·F76). 이전에는 Spring 메모리(Map)에 있어 재시작하면 받은 보상 쿠폰과 사용 처리가 사라졌고,
 * id를 System.currentTimeMillis() 로 만들어 같은 밀리초 두 장이 같은 id가 됐다. 이제 DB 행이고 id는 AUTO_INCREMENT.
 * 스키마: db/member_coupon.sql (운영 ddl-auto=validate).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_coupon",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_coupon_seed", columnNames = {"member_id", "seed_key"}),
        indexes = @Index(name = "idx_member_coupon_member", columnList = "member_id"))
@Entity
public class MemberCoupon extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    /** 시연 쿠폰이면 그 키(회원당 1장씩만 만들어지게 유니크), 보상 쿠폰은 NULL */
    @Column(name = "seed_key", length = 32)
    private String seedKey;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "condition_text", nullable = false)
    private String conditionText;

    /** 이 날짜까지 사용 가능(당일 포함) */
    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Builder
    private MemberCoupon(MemberEntity member, String seedKey, String title, String shopName, CouponType type,
                         String conditionText, LocalDate expiresAt, LocalDateTime usedAt, String thumbnailUrl) {
        this.member = member;
        this.seedKey = seedKey;
        this.title = title;
        this.shopName = shopName;
        this.type = type;
        this.conditionText = conditionText;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isUsed() { return usedAt != null; }

    public boolean isExpired(LocalDate today) { return expiresAt.isBefore(today); }
}
