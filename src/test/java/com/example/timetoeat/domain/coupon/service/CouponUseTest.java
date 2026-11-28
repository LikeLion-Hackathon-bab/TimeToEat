package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.model.CouponStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/** F76 쿠폰 사용. 기존 서비스는 use()가 TODO라 사용 후에도 '사용 전'에 남았다. */
class CouponUseTest {
    private final CouponQueryService query = new CouponQueryService();
    private final CouponCommandService command = new CouponCommandService(query, e -> {});

    private int status(Runnable r) {
        try { r.run(); return 204; } catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }

    @Test
    void usedCouponMovesToUsedAndCannotBeUsedTwice() {
        long me = 7L;
        assertTrue(query.getMyCoupons(me, CouponStatus.UNUSED).stream().anyMatch(c -> c.getCouponId() == 1L));
        assertEquals(204, status(() -> command.use(me, 1L)));
        assertTrue(query.getMyCoupons(me, CouponStatus.USED).stream().anyMatch(c -> c.getCouponId() == 1L));
        assertFalse(query.getMyCoupons(me, CouponStatus.UNUSED).stream().anyMatch(c -> c.getCouponId() == 1L));
        assertEquals(409, status(() -> command.use(me, 1L)));
        assertEquals(404, status(() -> command.use(me, 999L)));
        assertEquals(401, status(() -> command.use(null, 1L)));
        // 다른 회원에게는 영향 없음
        assertTrue(query.getMyCoupons(8L, CouponStatus.UNUSED).stream().anyMatch(c -> c.getCouponId() == 1L));
    }
}
