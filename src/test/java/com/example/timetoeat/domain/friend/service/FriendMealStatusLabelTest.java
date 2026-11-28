package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.friend.dto.response.FriendMealItemResponse;
import com.example.timetoeat.domain.meal.entity.MemberMealStatus;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** F38·F39 친구 식사 상태 목록: 4시간 창, 수동 공복, 필터, 시연 채움, 프로필 이미지 대체값. 고정 KST 시계로 계산. */
class FriendMealStatusLabelTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);
    private final Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private MemberJpaRepository members;
    private MemberMealStatusRepository meals;
    private FriendMealQueryService service;

    @BeforeEach
    void setUp() {
        members = mock(MemberJpaRepository.class);
        meals = mock(MemberMealStatusRepository.class);
        // 시연 채움 동작을 시험하므로 demo-friends.enabled=true 로 만든다(운영 기본값은 false)
        service = new FriendMealQueryService(clock, new FriendDirectory(members, mock(ReferralCodeRepository.class)), meals, true);
    }

    private MemberEntity member(long id, String name, String img) {
        return MemberEntity.builder().id(id).username(name).profileImageUrl(img).build();
    }

    private MemberMealStatus status(MemberEntity m, LocalDateTime last, LocalDateTime manual) {
        return MemberMealStatus.builder().member(m).lastMealAt(last).manualFastingSince(manual).build();
    }

    private FriendMealItemResponse only(MemberEntity m, MemberMealStatus s) {
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(1L)).thenReturn(List.of(m));
        when(meals.findByMember_IdIn(any())).thenReturn(s == null ? List.of() : List.of(s));
        return service.getDemoList(1L, "ALL").stream().filter(i -> i.getMemberId().equals(m.getId())).findFirst().orElseThrow();
    }

    @Test
    void noStatusRowMeansHungryWithoutHours() {
        var item = only(member(2, "가", null), null);
        assertTrue(item.isHungry());
        assertEquals("공복이에요", item.getLabel());
    }

    @Test
    void ateWithinFourHoursIsFull() {
        var m = member(2, "가", null);
        var item = only(m, status(m, NOW.minusHours(3).minusMinutes(59), null));
        assertFalse(item.isHungry());
        assertEquals("방금 먹었어요", item.getLabel());
    }

    @Test
    void exactlyFourHoursAfterMealIsHungryAgain() {
        var m = member(2, "가", null);
        var item = only(m, status(m, NOW.minusHours(4), null));
        assertTrue(item.isHungry(), "4시간이 지난 시점(경계 포함)부터 공복");
        assertEquals("4시간 공복이에요", item.getLabel());
    }

    @Test
    void hoursAreTruncatedNotRounded() {
        var m = member(2, "가", null);
        assertEquals("5시간 공복이에요", only(m, status(m, NOW.minusHours(5).minusMinutes(59), null)).getLabel());
    }

    @Test
    void manualFastingNewerThanMealWins() {
        var m = member(2, "가", null);
        var item = only(m, status(m, NOW.minusHours(1), NOW.minusMinutes(30)));
        assertTrue(item.isHungry(), "수동 OFF가 마지막 식사보다 최근이면 공복");
    }

    @Test
    void manualFastingOlderThanMealIsIgnored() {
        var m = member(2, "가", null);
        var item = only(m, status(m, NOW.minusHours(1), NOW.minusHours(6)));
        assertFalse(item.isHungry());
        assertEquals("방금 먹었어요", item.getLabel());
    }

    @Test
    void manualFastingWithoutMealCountsHoursFromManualTime() {
        var m = member(2, "가", null);
        assertEquals("3시간 공복이에요", only(m, status(m, null, NOW.minusHours(3))).getLabel());
    }

    @Test
    void blankProfileImageFallsBackToAvatarAndRealImageIsKept() {
        assertEquals("https://picsum.photos/seed/avatar2/80/80", only(member(2, "가", "  "), null).getProfileImageUrl());
        assertEquals("https://img/x.png", only(member(3, "나", "https://img/x.png"), null).getProfileImageUrl());
    }

    @Test
    void filtersSplitTheSameListAndAllIsTheirUnion() {
        var full = member(2, "배부름", null);
        var hungry = member(3, "배고픔", null);
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(1L)).thenReturn(List.of(full, hungry));
        when(meals.findByMember_IdIn(any())).thenReturn(List.of(status(full, NOW.minusHours(1), null)));

        var all = service.getDemoList(1L, "ALL");
        var h = service.getDemoList(1L, "HUNGRY");
        var n = service.getDemoList(1L, "NOT_HUNGRY");
        assertEquals(30, all.size());
        assertEquals(all.size(), h.size() + n.size());
        assertTrue(h.stream().allMatch(FriendMealItemResponse::isHungry));
        assertTrue(n.stream().noneMatch(FriendMealItemResponse::isHungry));
        assertTrue(n.stream().anyMatch(i -> i.getMemberId() == 2L));
        assertTrue(h.stream().anyMatch(i -> i.getMemberId() == 3L));
        assertEquals("배부름", all.get(0).getUserName(), "실제 회원이 시연 항목보다 먼저");
    }

    @Test
    void thirtyRealMembersMeansNoDemoEntries() {
        List<MemberEntity> list = new ArrayList<>();
        IntStream.rangeClosed(2, 31).forEach(i -> list.add(member(i, "m" + i, null)));
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(anyLong())).thenReturn(list);
        when(meals.findByMember_IdIn(any())).thenReturn(List.of());
        var all = service.getDemoList(1L, "ALL");
        assertEquals(30, all.size());
        assertTrue(all.stream().allMatch(i -> i.getMemberId() > 0));
    }

    @Test
    void demoEntriesAreStableWithinTheSameDay() {
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(meals.findByMember_IdIn(any())).thenReturn(List.of());
        var a = service.getDemoList(1L, "ALL");
        var b = service.getDemoList(1L, "ALL");
        assertEquals(30, a.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).isHungry(), b.get(i).isHungry());
            assertEquals(a.get(i).getLabel(), b.get(i).getLabel());
        }
    }

    @Test
    void theRequestingMemberIsExcludedAtTheQuery() {
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(meals.findByMember_IdIn(any())).thenReturn(List.of());
        service.getDemoList(77L, "ALL");
        verify(members).findTop30ByIdNotOrderByCreatedAtDesc(77L);
    }
}
