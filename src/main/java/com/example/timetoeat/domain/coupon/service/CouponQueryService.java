package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * 쿠폰함 조회. 쿠폰은 member_coupon 테이블에 있다(재시작해도 남음).
 * 트랜잭션을 걸지 않는다: 시연 쿠폰 생성(별도 트랜잭션)이 커밋된 뒤의 행을 새 조회로 읽기 위해서.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponQueryService {

    private final MemberCouponRepository coupons;
    private final CouponSeeder seeder;

    public List<CouponResponse> getMyCoupons(Long memberId, CouponStatus status) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            seeder.ensureSeeded(memberId);
        } catch (DataIntegrityViolationException raced) {
            // 같은 회원의 다른 요청이 먼저 시연 쿠폰을 만들었다 → 그 행을 그대로 읽는다
            log.debug("coupon seed race for member {}", memberId);
        }
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
