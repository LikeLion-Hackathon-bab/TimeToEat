package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 친구 = 추천 코드 수령 관계 우선 + 최근 가입자, 합집합 최대 30명, 나·0 이하 id 제외. */
class FriendDirectoryTest {
    private static MemberEntity m(long id) { return MemberEntity.builder().id(id).username("m" + id).build(); }

    @Test
    void referralFriendsComeFirstThenRecentMembersUpTo30() {
        var members = mock(MemberJpaRepository.class);
        var referrals = mock(ReferralCodeRepository.class);
        when(referrals.findFriendIds(1L)).thenReturn(Set.of(500L, 7L));
        when(members.findAllById(any())).thenReturn(List.of(m(7), m(500)));
        // 최근 가입 30명: 100..129 (7 은 중복으로 한 번 더 나옴)
        List<MemberEntity> recent = new ArrayList<>(LongStream.range(100, 129).mapToObj(FriendDirectoryTest::m).toList());
        recent.add(0, m(7));
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(1L)).thenReturn(recent);

        var ids = new FriendDirectory(members, referrals).friendIdsOf(1L);

        assertEquals(30, ids.size());
        assertEquals(List.of(500L, 7L), ids.subList(0, 2), "추천 관계 먼저(최근 친구부터)");
        assertEquals(ids.size(), Set.copyOf(ids).size(), "중복 없음");
        assertFalse(ids.contains(128L), "30명 상한 때문에 최근 가입자 끝은 밀려남");
        assertFalse(ids.contains(1L));
    }

    @Test
    void withoutOtherMembersThereAreNoFriends() {
        var members = mock(MemberJpaRepository.class);
        var referrals = mock(ReferralCodeRepository.class);
        when(referrals.findFriendIds(1L)).thenReturn(Set.of());
        when(members.findTop30ByIdNotOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        assertTrue(new FriendDirectory(members, referrals).friendIdsOf(1L).isEmpty());
        verify(members, never()).findAllById(any());
    }
}
