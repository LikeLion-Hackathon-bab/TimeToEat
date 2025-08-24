package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CouponCommandService {

    private final CouponQueryService couponQueryService;

    public void use(Long memberId, Long couponId) {
        // TODO: 실제 구현
    }

    // 보상 쿠폰 발급(데모) → 쿠폰함에 즉시 반영
    public void issueRewardCoupon(Long memberId, String type) {
        String title;
        switch (type == null ? "" : type.toUpperCase()) {
            case "WEEK"  -> title = "주간 챌린지 리워드";
            case "MONTH" -> title = "월간 챌린지 리워드";
            case "INVITE"-> title = "친구 초대 리워드";
            default      -> title = "리워드 쿠폰";
        }

        LocalDate expires = LocalDate.now().plusDays(30);
        long id = System.currentTimeMillis(); // 데모용 임시 ID

        CouponResponse reward = CouponResponse.builder()
                .couponId(id)
                .title(title)
                .shopName("모든 밥먹댕 제휴점")
                .type(CouponResponse.CouponType.SERVICE)
                .condition("매장 방문 시 2,000원 할인 쿠폰 사용 가능")
                .expiresAt(expires)
                .used(false)
                .thumbnailUrl("https://picsum.photos/seed/reward" + id + "/200/120")
                .build();

        couponQueryService.addReward(memberId, reward);
    }
}
