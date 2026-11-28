package com.example.timetoeat.domain.coupon.service;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse.CouponType;
import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import com.example.timetoeat.domain.coupon.repository.MemberCouponRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** F76 쿠폰 사용 판정(저장소 대역). 실제 DB 동작·재시작 후 유지는 qa/CouponPersistenceIT. */
class CouponUseTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);
    private final MemberCouponRepository repo = mock(MemberCouponRepository.class);
    private final CouponCommandService command = new CouponCommandService(repo, mock(MemberJpaRepository.class),
            Clock.fixed(NOW.atZone(KST).toInstant(), KST));

    private MemberCoupon coupon(LocalDate expires, LocalDateTime usedAt) {
        return MemberCoupon.builder().member(MemberEntity.builder().id(7L).build()).title("t").shopName("s")
                .type(CouponType.SERVICE).conditionText("c").expiresAt(expires).usedAt(usedAt).build();
    }

    private int status(Runnable r) {
        try { r.run(); return 204; } catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }

    @Test
    void useMarksOnlyThroughConditionalUpdate() {
        when(repo.findByIdAndMember_Id(1L, 7L)).thenReturn(Optional.of(coupon(NOW.toLocalDate().plusDays(3), null)));
        when(repo.markUsedIfUnused(1L, 7L, NOW)).thenReturn(1);
        assertEquals(204, status(() -> command.use(7L, 1L)));
        verify(repo).markUsedIfUnused(1L, 7L, NOW);
    }

    @Test
    void statusCodes() {
        assertEquals(401, status(() -> command.use(null, 1L)));
        when(repo.findByIdAndMember_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
        assertEquals(404, status(() -> command.use(7L, 999L)), "없는 쿠폰·남의 쿠폰");
        when(repo.findByIdAndMember_Id(2L, 7L)).thenReturn(Optional.of(coupon(NOW.toLocalDate().plusDays(3), NOW.minusDays(1))));
        assertEquals(409, status(() -> command.use(7L, 2L)), "이미 사용");
        when(repo.findByIdAndMember_Id(3L, 7L)).thenReturn(Optional.of(coupon(NOW.toLocalDate().minusDays(1), null)));
        assertEquals(409, status(() -> command.use(7L, 3L)), "만료");
        when(repo.findByIdAndMember_Id(4L, 7L)).thenReturn(Optional.of(coupon(NOW.toLocalDate(), null)));
        when(repo.markUsedIfUnused(eq(4L), eq(7L), any())).thenReturn(1);
        assertEquals(204, status(() -> command.use(7L, 4L)), "당일 만료는 사용 가능");
        when(repo.findByIdAndMember_Id(5L, 7L)).thenReturn(Optional.of(coupon(NOW.toLocalDate().plusDays(1), null)));
        when(repo.markUsedIfUnused(eq(5L), eq(7L), any())).thenReturn(0);
        assertEquals(409, status(() -> command.use(7L, 5L)), "동시 요청에 진 쪽");
        verify(repo, never()).markUsedIfUnused(eq(2L), anyLong(), any());
        verify(repo, never()).markUsedIfUnused(eq(3L), anyLong(), any());
    }
}
