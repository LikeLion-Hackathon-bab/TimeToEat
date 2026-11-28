package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.dto.response.CouponResponse.CouponType;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import org.junit.jupiter.api.Test;
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
 * F75·F76 쿠폰 보관함: 빈 쿠폰함, 정렬·필터, 보상 쿠폰 발급, 만료 문구. 저장소 대역.
 * (쿠폰이 DB로 옮겨져(T1) 사용 판정은 CouponUseTest, 실제 DB의 동시 사용·재시작 후 유지는 qa/CouponPersistenceIT)
 */
class CouponBoxTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private final Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private final MemberCouponRepository repo = mock(MemberCouponRepository.class);
    private final MemberJpaRepository members = mock(MemberJpaRepository.class);
    private final CouponQueryService query = new CouponQueryService(repo);
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

    /** F76/D07: 새 회원의 쿠폰함은 빈 배열(예전의 하드코딩 가짜 쿠폰 3장 없음), 저장소에 아무것도 쓰지 않는다. */
    @Test
    void newMemberHasEmptyCouponBox() {
        when(repo.findAllByMember_Id(8L)).thenReturn(List.of());
        assertEquals(List.of(), query.getMyCoupons(8L, null));
        assertEquals(List.of(), query.getMyCoupons(8L, CouponStatus.UNUSED));
        assertEquals(List.of(), query.getMyCoupons(8L, CouponStatus.USED));
        verify(repo, never()).save(any());
        verify(repo, never()).saveAll(any());
        assertEquals(401, status(() -> query.getMyCoupons(null, null)));
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
        assertTrue(r.stream().allMatch(c -> c.getThumbnailUrl() == null), "외부 picsum 자리 이미지 없음");
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
