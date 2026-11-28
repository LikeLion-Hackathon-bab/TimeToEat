package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** F75·F76 쿠폰 보관함: 정렬·필터, 보상 쿠폰 발급, 만료·동시 사용. (CouponUseTest 의 기본 사용 흐름은 중복하지 않는다) */
class CouponBoxTest {
    private final CouponQueryService query = new CouponQueryService();
    private final CouponCommandService command = new CouponCommandService(query);

    private static CouponResponse coupon(long id, LocalDate expires, boolean used) {
        return CouponResponse.builder().couponId(id).title("t" + id).shopName("s").type(CouponResponse.CouponType.SERVICE)
                .condition("c").expiresAt(expires).used(used).thumbnailUrl("u").build();
    }

    @Test
    void allListPutsUnusedFirstThenSoonestExpiry() {
        List<CouponResponse> all = query.getMyCoupons(7L, null);
        assertEquals(3, all.size(), "시드 3장");
        int firstUsed = -1;
        for (int i = 0; i < all.size(); i++) if (all.get(i).isUsed()) { firstUsed = i; break; }
        if (firstUsed >= 0) for (int i = firstUsed; i < all.size(); i++) assertTrue(all.get(i).isUsed(), "사용 전 뒤에 사용 완료만");
        for (int i = 1; i < all.size(); i++) {
            if (all.get(i - 1).isUsed() == all.get(i).isUsed())
                assertFalse(all.get(i - 1).getExpiresAt().isAfter(all.get(i).getExpiresAt()), "같은 그룹은 만료 임박순");
        }
    }

    @Test
    void unusedAscendingAndUsedDescendingAndTogetherTheyEqualAll() {
        long me = 42L;
        command.use(me, 1L);
        var unused = query.getMyCoupons(me, CouponStatus.UNUSED);
        var used = query.getMyCoupons(me, CouponStatus.USED);
        assertEquals(query.getMyCoupons(me, null).size(), unused.size() + used.size());
        assertTrue(unused.stream().noneMatch(CouponResponse::isUsed));
        assertTrue(used.stream().allMatch(CouponResponse::isUsed));
        assertEquals(unused.stream().sorted(Comparator.comparing(CouponResponse::getExpiresAt)).toList(), unused);
        assertEquals(used.stream().sorted(Comparator.comparing(CouponResponse::getExpiresAt).reversed()).toList(), used);
    }

    @Test
    void seedIsDeterministicPerMember() {
        var a = query.getMyCoupons(5L, null).stream().map(c -> c.getCouponId() + ":" + c.isUsed()).toList();
        var b = new CouponQueryService().getMyCoupons(5L, null).stream().map(c -> c.getCouponId() + ":" + c.isUsed()).toList();
        assertEquals(a, b, "같은 회원이면 재시작해도 시드 사용 여부가 같다");
    }

    @Test
    void rewardTitlesByTypeAndRewardIsUsableOnceByOwnerOnly() {
        command.issueRewardCoupon(9L, "week");
        command.issueRewardCoupon(9L, "MONTH");
        command.issueRewardCoupon(9L, "INVITE");
        command.issueRewardCoupon(9L, null);
        var titles = query.getMyCoupons(9L, CouponStatus.UNUSED).stream().map(CouponResponse::getTitle).toList();
        assertTrue(titles.containsAll(List.of("주간 챌린지 리워드", "월간 챌린지 리워드", "친구 초대 리워드", "리워드 쿠폰")), titles.toString());

        var reward = query.getMyCoupons(9L, null).stream().filter(c -> c.getTitle().equals("친구 초대 리워드")).findFirst().orElseThrow();
        assertEquals(LocalDate.now().plusDays(30), reward.getExpiresAt());
        assertEquals(404, status(() -> command.use(10L, reward.getCouponId())), "다른 회원은 남의 보상 쿠폰을 쓸 수 없다");
        assertEquals(204, status(() -> command.use(9L, reward.getCouponId())));
        assertEquals(409, status(() -> command.use(9L, reward.getCouponId())));
    }

    @Test
    void expiredCouponIsRejectedWith409() {
        query.addReward(3L, coupon(500L, LocalDate.now().minusDays(1), false));
        var e = assertThrows(ResponseStatusException.class, () -> command.use(3L, 500L));
        assertEquals(409, e.getStatusCode().value());
        assertEquals("만료된 쿠폰입니다.", e.getReason());
    }

    @Test
    void couponExpiringTodayIsStillUsable() {
        query.addReward(3L, coupon(501L, LocalDate.now(), false));
        assertEquals(204, status(() -> command.use(3L, 501L)));
    }

    @Test
    void concurrentUseSucceedsExactlyOnce() throws Exception {
        int n = 16;
        var pool = Executors.newFixedThreadPool(n);
        var start = new CountDownLatch(1);
        var ok = new AtomicInteger(); var conflict = new AtomicInteger();
        List<Future<?>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) fs.add(pool.submit(() -> {
            start.await();
            int s = status(() -> command.use(11L, 1L));
            if (s == 204) ok.incrementAndGet(); else if (s == 409) conflict.incrementAndGet();
            return null;
        }));
        start.countDown();
        for (var f : fs) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(1, ok.get());
        assertEquals(n - 1, conflict.get());
    }

    private static int status(Runnable r) {
        try { r.run(); return 204; } catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }
}
