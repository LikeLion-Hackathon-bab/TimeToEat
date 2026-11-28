package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.friend.dto.response.FriendMealItemResponse;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** D31: 시연용 채움 항목 id(1000+n)가 실제 회원 id와 겹치면 그 카드로 보낸 초대가 다른 실제 회원에게 간다. */
class FriendMealQueryServiceTest {
    @Test
    void demoEntriesNeverReuseARealMemberId() {
        var friendDir = mock(FriendDirectory.class);
        var meals = mock(MemberMealStatusRepository.class);
        // 실제 회원 2명이 id 1002, 1003 (AUTO_INCREMENT가 1000을 넘은 DB)
        when(friendDir.friendsOf(anyLong())).thenReturn(List.of(
                MemberEntity.builder().id(1002L).username("실제A").build(),
                MemberEntity.builder().id(1003L).username("실제B").build()));
        when(meals.findByMember_IdIn(any())).thenReturn(List.of());
        var service = new FriendMealQueryService(Clock.system(ZoneId.of("Asia/Seoul")), friendDir, meals, true);

        List<FriendMealItemResponse> list = service.getDemoList(1L, "ALL");

        assertEquals(30, list.size(), "DEMO_FRIENDS_ENABLED=true 면 시연 채움 정책(30명)은 유지");
        var ids = new HashSet<Long>();
        for (var item : list) assertTrue(ids.add(item.getMemberId()), "id " + item.getMemberId() + " 중복");
        list.stream().filter(i -> !i.getUserName().startsWith("실제"))
                .forEach(i -> assertTrue(i.getMemberId() < 0, "시연 항목 " + i.getUserName() + " id=" + i.getMemberId()
                        + " 는 실제 회원 id 범위(양수)를 쓰면 안 된다"));
    }

    @Test
    void byDefaultTheListHasOnlyRealMembers() {
        var friendDir = mock(FriendDirectory.class);
        var meals = mock(MemberMealStatusRepository.class);
        when(friendDir.friendsOf(anyLong())).thenReturn(List.of(
                MemberEntity.builder().id(7L).username("실제A").build()));
        when(meals.findByMember_IdIn(any())).thenReturn(List.of());
        var service = new FriendMealQueryService(Clock.system(ZoneId.of("Asia/Seoul")), friendDir, meals, false);

        List<FriendMealItemResponse> list = service.getDemoList(1L, "ALL");

        assertEquals(1, list.size(), "가짜 시연 친구로 채우지 않는다");
        assertEquals(7L, list.get(0).getMemberId());
    }
}
