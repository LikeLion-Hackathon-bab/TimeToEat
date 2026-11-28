package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CouponCommandService {

    private final MemberCouponRepository coupons;
    private final MemberJpaRepository members;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;

    /** 쿠폰 사용(F76). 로그인 없음 401, 없는(남의) 쿠폰 404, 이미 사용 409, 만료 409(당일까지 사용 가능). */
    public void use(Long memberId, Long couponId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        MemberCoupon coupon = coupons.findByIdAndMember_Id(couponId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
        if (coupon.isUsed()) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용한 쿠폰입니다.");
        if (coupon.isExpired(LocalDate.now(clock)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "만료된 쿠폰입니다.");
        // 동시 사용 요청: 조건부 UPDATE 로 한 건만 성공
        if (coupons.markUsedIfUnused(couponId, memberId, LocalDateTime.now(clock)) != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용한 쿠폰입니다.");
    }

    /**
     * 보상 쿠폰 발급 → 쿠폰함에 저장. 호출한 쪽(챌린지 보상·추천 등록)의 트랜잭션에 같이 묶여, 보상 기록과 쿠폰이 함께 커밋·롤백된다.
     * id는 DB AUTO_INCREMENT(이전 System.currentTimeMillis() 는 같은 밀리초 두 장이 같은 id라 한 장을 쓰면 둘 다 사용 처리됐다).
     */
    public CouponResponse issueRewardCoupon(Long memberId, String type) {
        String key = type == null ? "" : type.toUpperCase();
        String title = switch (key) {
            case "WEEK"   -> "주간 챌린지 리워드";
            case "MONTH"  -> "월간 챌린지 리워드";
            case "INVITE" -> "친구 초대 리워드";
            default       -> "리워드 쿠폰";
        };
        MemberCoupon saved = coupons.save(MemberCoupon.builder()
                .member(members.getReferenceById(memberId))
                .title(title)
                .shopName("모든 밥먹댕 제휴점")
                .type(CouponResponse.CouponType.SERVICE)
                .conditionText("매장 방문 시 2,000원 할인 쿠폰 사용 가능")
                .expiresAt(LocalDate.now(clock).plusDays(30))
                // 썸네일 없음(null): 제휴점 공통 보상이라 가게 사진이 없다. 외부 picsum 자리 이미지는 쓰지 않는다.
                .build());
        events.publishEvent(new com.example.timetoeat.domain.notification.event.NotificationEvents.CouponIssued(
                memberId, saved.getId(), title)); // 커밋 뒤 쿠폰 알림(#133)
        return CouponQueryService.toResponse(saved);
    }
}
