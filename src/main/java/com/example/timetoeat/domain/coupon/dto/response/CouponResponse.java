package com.example.timetoeat.domain.coupon.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponResponse {

    private Long couponId;
    private String title;
    private String shopName;
    private CouponType type;  // DISCOUNT(할인 쿠폰) / SERVICE(서비스 쿠폰)
    private String condition;  // 사용조건 요약
    private LocalDate expiresAt;  // 유효기간
    private boolean used;  // 사용 여부
    private String thumbnailUrl;  // 썸네일

    @Builder
    private CouponResponse(Long couponId, String title, String shopName, CouponType type,
                           String condition, LocalDate expiresAt, boolean used, String thumbnailUrl) {
        this.couponId = couponId;
        this.title = title;
        this.shopName = shopName;
        this.type = type;
        this.condition = condition;
        this.expiresAt = expiresAt;
        this.used = used;
        this.thumbnailUrl = thumbnailUrl;
    }

    public enum CouponType { DISCOUNT, SERVICE }
}
