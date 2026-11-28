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

    // 데모용: 사용 처리한 쿠폰 id(쿠폰 저장소가 메모리라 같은 수명). 영속 쿠폰은 쿠폰 테이블 도입 시 함께.
    private final Map<Long, Set<Long>> usedBox = new ConcurrentHashMap<>();

    /** 쿠폰 사용(F76). 없는 쿠폰 404, 이미 사용 409, 만료 409. */
    public synchronized void markUsed(Long memberId, Long couponId) {
        if (memberId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED);
        var coupon = all(memberId).stream().filter(c -> c.getCouponId().equals(couponId)).findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
        if (coupon.isUsed()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "이미 사용한 쿠폰입니다.");
        if (coupon.getExpiresAt().isBefore(LocalDate.now())) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "만료된 쿠폰입니다.");
        usedBox.computeIfAbsent(memberId, k -> ConcurrentHashMap.newKeySet()).add(couponId);
    }

    private List<CouponResponse> all(Long memberId) {
        List<CouponResponse> all = new ArrayList<>(seed(memberId));
        all.addAll(rewardBox.getOrDefault(memberId, List.of()));
        Set<Long> used = usedBox.getOrDefault(memberId, Set.of());
        return all.stream().map(c -> !c.isUsed() && used.contains(c.getCouponId()) ? CouponResponse.builder()
                .couponId(c.getCouponId()).title(c.getTitle()).shopName(c.getShopName()).type(c.getType())
                .condition(c.getCondition()).expiresAt(c.getExpiresAt()).used(true).thumbnailUrl(c.getThumbnailUrl())
                .build() : c).toList();
    }

    public List<CouponResponse> getMyCoupons(Long memberId, CouponStatus status) {
        List<CouponResponse> all = all(memberId);

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
