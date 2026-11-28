package com.example.timetoeat.qa;

import com.example.timetoeat.TimeToEatApplication;
import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.coupon.service.CouponQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** 쿠폰함 DB 저장(F75·F76, T1): 고유 id, 사용 1회, 재시작(새 컨텍스트, ddl-auto=validate + db/member_coupon.sql) 뒤 유지. */
class CouponPersistenceIT extends QaIntegrationSupport {
    @Autowired CouponQueryService query;
    @Autowired CouponCommandService command;
    @Autowired DataSource dataSource;

    private List<CouponResponse> rewards(long memberId, CouponStatus status) {
        return query.getMyCoupons(memberId, status).stream().filter(c -> c.getTitle().endsWith("리워드")).toList();
    }

    @Test
    void rewardCouponsIssuedBackToBackGetDistinctIds() {
        long me = member("쿠폰1").getId();
        for (int i = 0; i < 200; i++) command.issueRewardCoupon(me, i % 2 == 0 ? "WEEK" : "INVITE");
        var ids = rewards(me, null).stream().map(CouponResponse::getCouponId).toList();
        assertEquals(200, ids.size());
        assertEquals(200, new HashSet<>(ids).size(), "보상 쿠폰 id 중복");
    }

    @Test
    void usingOneRewardDoesNotConsumeAnotherIssuedInTheSameMillisecond() {
        long me = member("쿠폰2").getId();
        command.issueRewardCoupon(me, "WEEK");
        command.issueRewardCoupon(me, "MONTH");
        var r = rewards(me, CouponStatus.UNUSED);
        assertEquals(2, r.size());
        command.use(me, r.get(0).getCouponId());
        assertEquals(1, rewards(me, CouponStatus.UNUSED).size(), "한 장만 썼는데 다른 장도 사용 처리됨");
        assertEquals(r.get(0).getCouponId(), rewards(me, CouponStatus.USED).get(0).getCouponId());
    }

    @Test
    void seedCouponsAreCreatedOncePerMemberAndOthersAreUnaffected() throws Exception {
        long me = member("쿠폰3").getId(), other = member("쿠폰4").getId();
        // 첫 조회가 겹쳐도 시연 쿠폰은 3장
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        var futures = new java.util.ArrayList<Future<?>>();
        for (int i = 0; i < 8; i++) futures.add(pool.submit(() -> { start.await(); return query.getMyCoupons(me, null); }));
        start.countDown();
        for (var f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        var mine = query.getMyCoupons(me, null);
        assertEquals(3, mine.size());
        long seed1 = mine.stream().filter(c -> c.getTitle().equals("세겹 먹는 날")).findFirst().orElseThrow().getCouponId();
        command.use(me, seed1);
        assertTrue(query.getMyCoupons(me, CouponStatus.USED).stream().anyMatch(c -> c.getCouponId() == seed1));
        assertEquals(409, code(() -> command.use(me, seed1)));
        assertEquals(404, code(() -> command.use(other, seed1)), "남의 쿠폰");
        assertTrue(query.getMyCoupons(other, CouponStatus.UNUSED).stream().anyMatch(c -> c.getTitle().equals("세겹 먹는 날")));
    }

    @Test
    void concurrentUseSucceedsExactlyOnce() throws Exception {
        long me = member("쿠폰5").getId();
        long id = command.issueRewardCoupon(me, "WEEK").getCouponId();
        var ok = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(16);
        var start = new CountDownLatch(1);
        var futures = new java.util.ArrayList<Future<?>>();
        for (int i = 0; i < 16; i++) futures.add(pool.submit(() -> {
            start.await();
            try { command.use(me, id); ok.incrementAndGet(); } catch (ResponseStatusException e) { assertEquals(409, e.getStatusCode().value()); }
            return null;
        }));
        start.countDown();
        for (var f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(1, ok.get());
    }

    /** 운영 절차대로 db/member_coupon.sql 로 테이블을 만들고, 쿠폰을 받고·쓴 뒤 새 애플리케이션(validate)을 띄워도 그대로 있다. */
    @Test
    void couponsSurviveRestartWithMigrationSql() {
        jdbc.execute("drop table if exists member_coupon");
        new ResourceDatabasePopulator(new FileSystemResource("db/member_coupon.sql")).execute(dataSource);

        long me = member("쿠폰6").getId();
        long reward = command.issueRewardCoupon(me, "INVITE").getCouponId();
        long seed1 = query.getMyCoupons(me, null).stream().filter(c -> c.getTitle().equals("세겹 먹는 날"))
                .findFirst().orElseThrow().getCouponId();
        command.use(me, seed1);

        try (var restarted = new SpringApplicationBuilder(TimeToEatApplication.class).properties(
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.main.web-application-type=none",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.jpa.show-sql=false",
                "logging.level.org.hibernate.SQL=warn").run()) {
            var q2 = restarted.getBean(CouponQueryService.class);
            var all = q2.getMyCoupons(me, null);
            assertEquals(4, all.size(), "시연 3 + 보상 1, 재시작 뒤 시연 쿠폰이 다시 생기지 않음");
            assertTrue(all.stream().anyMatch(c -> c.getCouponId() == reward && !c.isUsed() && c.getTitle().equals("친구 초대 리워드")));
            assertTrue(all.stream().anyMatch(c -> c.getCouponId() == seed1 && c.isUsed()), "사용 처리도 유지");
        }
    }

    private static int code(Runnable r) {
        try { r.run(); return 204; } catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }
}
