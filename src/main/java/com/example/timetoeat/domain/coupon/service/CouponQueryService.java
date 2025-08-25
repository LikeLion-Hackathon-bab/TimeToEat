package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.dto.response.CouponResponse.CouponType;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryService {

    // 데모용
    private final Map<Long, List<CouponResponse>> rewardBox = new ConcurrentHashMap<>();
    public void addReward(Long memberId, CouponResponse coupon) {
        rewardBox.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(coupon);
    }

    public List<CouponResponse> getMyCoupons(Long memberId, CouponStatus status) {
        List<CouponResponse> all =  new ArrayList<>(seed(memberId));
        all.addAll(rewardBox.getOrDefault(memberId, List.of()));

        if (status == null) {
            // 전체: UNUSED 먼저, 같은 그룹 내에서는 만료 임박순
            return all.stream()
                    .sorted(Comparator
                            .comparing(CouponResponse::isUsed)  // false(UNUSED) 먼저
                            .thenComparing(CouponResponse::getExpiresAt))  // 임박순
                    .toList();
        }

        boolean wantUsed = (status == CouponStatus.USED);
        Comparator<CouponResponse> cmp = wantUsed
                ? Comparator.comparing(CouponResponse::getExpiresAt, Comparator.reverseOrder()) // USED: 최근 만료 우선
                : Comparator.comparing(CouponResponse::getExpiresAt);  // UNUSED: 임박 우선

        return all.stream()
                .filter(c -> c.isUsed() == wantUsed)
                .sorted(cmp)
                .toList();
    }

    private List<CouponResponse> seed(Long memberId) {
        Random r = new Random(Objects.hashCode(memberId));
        LocalDate today = LocalDate.now();

        List<CouponResponse> list = new ArrayList<>();
        list.add(CouponResponse.builder()
                .couponId(1L).title("세겹 먹는 날").shopName("세겹 먹는 날")
                .type(CouponType.SERVICE).condition("삼겹살 2인분 이상 주문 시")
                .expiresAt(today.plusDays(30)).used(false)
                .thumbnailUrl("https://picsum.photos/seed/coupon1/200/120").build());

        list.add(CouponResponse.builder()
                .couponId(2L).title("동학 2000원 할인").shopName("동학")
                .type(CouponType.DISCOUNT).condition("2만원 이상 결제 시")
                .expiresAt(today.plusDays(14)).used(r.nextBoolean())
                .thumbnailUrl("https://picsum.photos/seed/coupon2/200/120").build());

        list.add(CouponResponse.builder()
                .couponId(3L).title("플렉스 버거(음료 서비스)").shopName("플렉스 버거")
                .type(CouponType.SERVICE).condition("버거 1개 이상 주문 시")
                .expiresAt(today.plusDays(45)).used(r.nextBoolean())
                .thumbnailUrl("https://picsum.photos/seed/coupon3/200/120").build());

        return list;
    }
}
