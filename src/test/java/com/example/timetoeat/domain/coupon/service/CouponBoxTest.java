package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.dto.response.CouponResponse.CouponType;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * F75·F76 쿠폰 보관함: 정렬·필터, 시연 쿠폰 생성, 보상 쿠폰 발급, 만료 문구. 저장소 대역.
 * (쿠폰이 DB로 옮겨져(T1) 사용 판정은 CouponUseTest, 실제 DB의 동시 사용·재시작 후 유지는 qa/CouponPersistenceIT)
 */
class CouponBoxTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private final Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private final MemberCouponRepository repo = mock(MemberCouponRepository.class);
    private final MemberJpaRepository members = mock(MemberJpaRepository.class);
    private final CouponSeeder seeder = mock(CouponSeeder.class);
    private final CouponQueryService query = new CouponQueryService(repo, seeder);
    private final CouponCommandService command = new CouponCommandService(repo, members, clock);

    private static MemberCoupon coupon(long id, LocalDate expires, boolean used) {
        var c = MemberCoupon.builder().member(MemberEntity.builder().id(7L).build()).title("t" + id).shopName("s")
                .type(CouponType.SERVICE).conditionText("c").expiresAt(expires).usedAt(used ? NOW : null).thumbnailUrl("u").build();
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private void box(MemberCoupon... cs) {
        when(repo.findAllByMember_Id(7L)).thenReturn(List.of(cs));
    }

    @Test
    void allListPutsUnusedFirstThenSoonestExpiry() {
        box(coupon(1, TODAY.plusDays(30), false), coupon(2, TODAY.plusDays(14), true), coupon(3, TODAY.plusDays(45), false),
                coupon(4, TODAY.plusDays(2), false), coupon(5, TODAY.plusDays(1), true));
        var ids = query.getMyCoupons(7L, null).stream().map(CouponResponse::getCouponId).toList();
        assertEquals(List.of(4L, 1L, 3L, 5L, 2L), ids);
        verify(seeder).ensureSeeded(7L);
    }

    @Test
    void unusedAscendingAndUsedDescendingAndTogetherTheyEqualAll() {
        box(coupon(1, TODAY.plusDays(30), false), coupon(2, TODAY.plusDays(14), true), coupon(3, TODAY.plusDays(45), true),
                coupon(4, TODAY.plusDays(2), false));
        var unused = query.getMyCoupons(7L, CouponStatus.UNUSED);
        var used = query.getMyCoupons(7L, CouponStatus.USED);
        assertEquals(query.getMyCoupons(7L, null).size(), unused.size() + used.size());
        assertTrue(unused.stream().noneMatch(CouponResponse::isUsed));
        assertTrue(used.stream().allMatch(CouponResponse::isUsed));
        assertEquals(unused.stream().sorted(Comparator.comparing(CouponResponse::getExpiresAt)).toList(), unused);
        assertEquals(used.stream().sorted(Comparator.comparing(CouponResponse::getExpiresAt).reversed()).toList(), used);
    }

    @Test
    void seedRaceIsSwallowedAndRowsAreRead() {
        doThrow(new DataIntegrityViolationException("uk_member_coupon_seed")).when(seeder).ensureSeeded(7L);
        box(coupon(1, TODAY.plusDays(30), false));
        assertEquals(1, query.getMyCoupons(7L, null).size());
        assertEquals(401, status(() -> query.getMyCoupons(null, null)));
    }

    @SuppressWarnings("unchecked")
    private List<MemberCoupon> seededFor(long memberId) {
        var coupons = mock(MemberCouponRepository.class);
        new CouponSeeder(coupons, members, clock).ensureSeeded(memberId);
        ArgumentCaptor<List<MemberCoupon>> saved = ArgumentCaptor.forClass(List.class);
        verify(coupons).saveAllAndFlush(saved.capture());
        return saved.getValue();
    }

    @Test
    void seedIsDeterministicPerMemberAndCreatedOnlyOnce() {
        var a = seededFor(5L).stream().map(c -> c.getSeedKey() + ":" + c.isUsed() + ":" + c.getExpiresAt()).toList();
        var b = seededFor(5L).stream().map(c -> c.getSeedKey() + ":" + c.isUsed() + ":" + c.getExpiresAt()).toList();
        assertEquals(a, b, "같은 회원이면 시연 쿠폰 사용 여부가 같다");
        assertEquals(List.of("DEMO_1", "DEMO_2", "DEMO_3"), seededFor(5L).stream().map(MemberCoupon::getSeedKey).toList());
        assertFalse(seededFor(5L).get(0).isUsed(), "1번 시연 쿠폰은 항상 사용 전");

        var already = mock(MemberCouponRepository.class);
        when(already.existsByMember_IdAndSeedKeyIsNotNull(5L)).thenReturn(true);
        new CouponSeeder(already, members, clock).ensureSeeded(5L);
        verify(already, never()).saveAllAndFlush(any());
    }

    @Test
    void rewardTitlesByTypeAndThirtyDayExpiry() {
        List<MemberCoupon> saved = new ArrayList<>();
        when(repo.save(any())).thenAnswer(i -> { MemberCoupon c = i.getArgument(0); ReflectionTestUtils.setField(c, "id", 100L + saved.size()); saved.add(c); return c; });
        var r = List.of(command.issueRewardCoupon(9L, "week"), command.issueRewardCoupon(9L, "MONTH"),
                command.issueRewardCoupon(9L, "INVITE"), command.issueRewardCoupon(9L, null));
        assertEquals(List.of("주간 챌린지 리워드", "월간 챌린지 리워드", "친구 초대 리워드", "리워드 쿠폰"),
                r.stream().map(CouponResponse::getTitle).toList());
        assertEquals(List.of(100L, 101L, 102L, 103L), r.stream().map(CouponResponse::getCouponId).toList());
        assertTrue(r.stream().allMatch(c -> c.getExpiresAt().equals(TODAY.plusDays(30)) && !c.isUsed()));
        assertTrue(saved.stream().allMatch(c -> c.getSeedKey() == null));
        verify(members, times(4)).getReferenceById(9L);
    }

    @Test
    void expiredCouponIsRejectedWith409() {
        when(repo.findByIdAndMember_Id(500L, 7L)).thenReturn(Optional.of(coupon(500, TODAY.minusDays(1), false)));
        var e = assertThrows(ResponseStatusException.class, () -> command.use(7L, 500L));
        assertEquals(409, e.getStatusCode().value());
        assertEquals("만료된 쿠폰입니다.", e.getReason());
    }

    private static int status(Runnable r) {
        try { r.run(); return 204; } catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }
}
