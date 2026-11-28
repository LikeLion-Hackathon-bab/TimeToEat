package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * 쿠폰함 조회. 쿠폰은 member_coupon 테이블에 있다(재시작해도 남음).
 * 실제로 발급된 쿠폰(챌린지 WEEK·MONTH 보상, 추천 등록 INVITE)만 돌려준다. 없으면 빈 배열.
 * (예전 메모리 시절의 하드코딩 가짜 쿠폰 "세겹 먹는 날"·"동학"·"플렉스 버거"와 picsum 썸네일은 없앴다: F76/D07)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryService {

    private final MemberCouponRepository coupons;

    public List<CouponResponse> getMyCoupons(Long memberId, CouponStatus status) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        List<CouponResponse> all = coupons.findAllByMember_Id(memberId).stream().map(CouponQueryService::toResponse).toList();

        if (status == null) {
            // 전체: UNUSED 먼저, 같은 그룹 내에서는 만료 임박순
            return all.stream()
                    .sorted(Comparator
                            .comparing(CouponResponse::isUsed)  // false(UNUSED) 먼저
                            .thenComparing(CouponResponse::getExpiresAt)  // 임박순
                            .thenComparing(CouponResponse::getCouponId))
                    .toList();
        }

        boolean wantUsed = (status == CouponStatus.USED);
        Comparator<CouponResponse> cmp = wantUsed
                ? Comparator.comparing(CouponResponse::getExpiresAt, Comparator.reverseOrder()) // USED: 최근 만료 우선
                : Comparator.comparing(CouponResponse::getExpiresAt);  // UNUSED: 임박 우선

        return all.stream()
                .filter(c -> c.isUsed() == wantUsed)
                .sorted(cmp.thenComparing(CouponResponse::getCouponId))
                .toList();
    }

    static CouponResponse toResponse(MemberCoupon c) {
        return CouponResponse.builder()
                .couponId(c.getId()).title(c.getTitle()).shopName(c.getShopName()).type(c.getType())
                .condition(c.getConditionText()).expiresAt(c.getExpiresAt()).used(c.isUsed())
                .thumbnailUrl(c.getThumbnailUrl())
                .build();
    }
}
