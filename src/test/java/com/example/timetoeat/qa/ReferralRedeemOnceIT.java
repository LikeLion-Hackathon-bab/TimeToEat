package com.example.timetoeat.qa;

import com.example.timetoeat.domain.coupon.service.CouponQueryService;
import com.example.timetoeat.domain.referral.exception.ReferralErrorCode;
import com.example.timetoeat.domain.referral.service.ReferralCommandService;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** T2: 추천 코드 등록은 회원당 1회. 순차·동시(서로 다른 코드) 모두 실제 MySQL 유니크(uk_referral_code_used_by)로 확인. */
class ReferralRedeemOnceIT extends QaIntegrationSupport {
    @Autowired ReferralCommandService referrals;
    @Autowired CouponQueryService coupons;

    private long inviteCoupons(long memberId) {
        return coupons.getMyCoupons(memberId, null).stream().filter(c -> c.getTitle().equals("친구 초대 리워드")).count();
    }

    @Test
    void secondCodeIsRejected() {
        var a = member("추천A"); var b = member("추천B"); var me = member("추천받음1");
        String codeA = referrals.create(a.getId()).getCode(), codeB = referrals.create(b.getId()).getCode();
        referrals.redeem(me.getId(), codeA);
        var e = assertThrows(CustomException.class, () -> referrals.redeem(me.getId(), codeB));
        assertEquals(ReferralErrorCode.ALREADY_REDEEMED, e.getBaseError());
        assertEquals(1, inviteCoupons(me.getId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from referral_code where code=? and used_by_id is not null", Integer.class, codeB));
    }

    @Test
    void concurrentRedeemsOfDifferentCodesSucceedOnce() throws Exception {
        var me = member("추천받음2");
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 6; i++) codes.add(referrals.create(member("추천인" + i).getId()).getCode());
        var ok = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(codes.size());
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<?>>();
        for (String code : codes) futures.add(pool.submit(() -> {
            start.await();
            try { referrals.redeem(me.getId(), code); ok.incrementAndGet(); }
            catch (CustomException e) { assertEquals(ReferralErrorCode.ALREADY_REDEEMED, e.getBaseError()); }
            return null;
        }));
        start.countDown();
        for (var f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(1, ok.get());
        assertEquals(1, jdbc.queryForObject("select count(*) from referral_code where used_by_id=?", Integer.class, me.getId()));
        assertEquals(1, inviteCoupons(me.getId()));
    }
}
