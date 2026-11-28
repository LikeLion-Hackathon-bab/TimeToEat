package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse.CouponType;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 시연 쿠폰 3장(제휴 쿠폰 테이블이 생기기 전 화면 채움)을 회원의 쿠폰함을 처음 볼 때 DB에 한 번 만든다.
 * 따로 트랜잭션(REQUIRES_NEW)에서 flush 해, 같은 회원의 첫 조회 두 건이 겹쳐 유니크(uk_member_coupon_seed)에
 * 걸려도 바깥 요청은 롤백되지 않고 이미 만들어진 행을 읽는다.
 */
@Component
@RequiredArgsConstructor
public class CouponSeeder {

    private final MemberCouponRepository coupons;
    private final MemberJpaRepository members;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSeeded(Long memberId) {
        if (coupons.existsByMember_IdAndSeedKeyIsNotNull(memberId)) return;
        var member = members.getReferenceById(memberId);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        // 예전 메모리 시드와 같은 값(회원마다 같은 결과): 2·3번은 회원 id로 정한 무작위 사용 여부
        Random r = new Random(Objects.hashCode(memberId));
        boolean used2 = r.nextBoolean(), used3 = r.nextBoolean();
        coupons.saveAllAndFlush(List.of(
                MemberCoupon.builder().member(member).seedKey("DEMO_1").title("세겹 먹는 날").shopName("세겹 먹는 날")
                        .type(CouponType.SERVICE).conditionText("삼겹살 2인분 이상 주문 시")
                        .expiresAt(today.plusDays(30)).thumbnailUrl("https://picsum.photos/seed/coupon1/200/120").build(),
                MemberCoupon.builder().member(member).seedKey("DEMO_2").title("동학 2000원 할인").shopName("동학")
                        .type(CouponType.DISCOUNT).conditionText("2만원 이상 결제 시")
                        .expiresAt(today.plusDays(14)).usedAt(used2 ? now : null)
                        .thumbnailUrl("https://picsum.photos/seed/coupon2/200/120").build(),
                MemberCoupon.builder().member(member).seedKey("DEMO_3").title("플렉스 버거(음료 서비스)").shopName("플렉스 버거")
                        .type(CouponType.SERVICE).conditionText("버거 1개 이상 주문 시")
                        .expiresAt(today.plusDays(45)).usedAt(used3 ? now : null)
                        .thumbnailUrl("https://picsum.photos/seed/coupon3/200/120").build()));
    }
}
