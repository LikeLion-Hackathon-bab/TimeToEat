package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.global.auth.demo.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** 친구 = 추천 코드 수령 관계만(최대 30명). 빈자리를 다른 회원으로 채우지 않는다. 시연 회원끼리는 서로 친구. */
class FriendDirectoryTest {
    private static MemberEntity m(long id) { return MemberEntity.builder().id(id).username("m" + id).email("m" + id + "@qa.test").build(); }
    private static MemberEntity demo(long id, int account) {
        return MemberEntity.builder().id(id).username("d" + account).email(DemoAccounts.email(account)).build();
    }

    @Test
    void onlyReferralFriendsNewestFirstNoFill() {
        var members = mock(MemberJpaRepository.class);
        var referrals = mock(ReferralCodeRepository.class);
        when(referrals.findFriendIds(1L)).thenReturn(Set.of(500L, 7L));
        when(members.findAllById(any())).thenReturn(List.of(m(7), m(500)));
        when(members.findById(1L)).thenReturn(Optional.of(m(1)));

        var ids = new FriendDirectory(members, referrals).friendIdsOf(1L);

        assertEquals(List.of(500L, 7L), ids, "추천 친구만, 최근 친구부터");
        verify(members, never()).findByEmailIn(anyCollection());
    }

    @Test
    void referralFriendsAreCappedAt30() {
        var members = mock(MemberJpaRepository.class);
        var referrals = mock(ReferralCodeRepository.class);
        var many = LongStream.rangeClosed(100, 140).boxed().collect(java.util.stream.Collectors.toSet());
        when(referrals.findFriendIds(1L)).thenReturn(many);
        when(members.findAllById(any())).thenAnswer(i -> {
            Iterable<Long> asked = i.getArgument(0);
            var out = new java.util.ArrayList<MemberEntity>();
            asked.forEach(id -> out.add(m(id)));
            return out;
        });
        when(members.findById(1L)).thenReturn(Optional.of(m(1)));

        var ids = new FriendDirectory(members, referrals).friendIdsOf(1L);

        assertEquals(FriendDirectory.LIMIT, ids.size());
        assertEquals(140L, ids.get(0));
        assertFalse(ids.contains(110L), "오래된 추천 친구는 30명 밖으로 밀림");
    }

    @Test
    void memberWithoutReferralsHasNoFriends() {
        var members = mock(MemberJpaRepository.class);
        var referrals = mock(ReferralCodeRepository.class);
        when(referrals.findFriendIds(1L)).thenReturn(Set.of());
        when(members.findById(1L)).thenReturn(Optional.of(m(1)));

        assertTrue(new FriendDirectory(members, referrals).friendIdsOf(1L).isEmpty());
        verify(members, never()).findByEmailIn(anyCollection());
    }

    @Test
    void demoMembersSeeEachOtherButNotThemselves() {
        var members = mock(MemberJpaRepository.class);
        var referrals = mock(ReferralCodeRepository.class);
        when(referrals.findFriendIds(11L)).thenReturn(Set.of());
        when(members.findById(11L)).thenReturn(Optional.of(demo(11, 1)));
        when(members.findByEmailIn(anyCollection())).thenReturn(List.of(demo(11, 1), demo(12, 2), demo(13, 3)));

        assertEquals(List.of(12L, 13L), new FriendDirectory(members, referrals).friendIdsOf(11L));
    }
}
