package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.timetoeat.global.auth.demo.DemoAccounts;

import java.util.*;
import java.util.stream.IntStream;

/**
 * "내 친구"의 유일한 출처. 친구 목록 화면(/friends/me/meals)·태그 후보와 홈 피드(F09)가 같이 쓴다.
 * 그래서 목록에 보이는 사람 = 피드에 글이 보이는 사람.
 *
 * 친구 = 추천 코드 수령 관계(양방향, 미수령 코드 제외), 최대 {@value #LIMIT}명.
 * 전에는 빈자리를 최근 가입 회원으로 채워서 모르는 사람의 글이 홈 피드에 보였다. 이제 채우지 않는다.
 * 시연 회원(시연 로그인 5명)끼리는 추천 관계 없이도 서로 친구로 본다. 시연에서 친구 기능을 보여 주기 위해서다.
 * 나 자신과 실제 회원이 아닌 id(0 이하)는 들어가지 않는다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendDirectory {
    public static final int LIMIT = 30;
    private static final List<String> DEMO_EMAILS =
            IntStream.rangeClosed(1, 5).mapToObj(DemoAccounts::email).toList();

    private final MemberJpaRepository memberRepo;
    private final ReferralCodeRepository referralRepo;

    /** 친구 회원(추천 관계는 최근 친구부터, 그다음 시연 회원끼리). */
    public List<MemberEntity> friendsOf(Long memberId) {
        // 추천 관계는 최근 친구(큰 id)부터. 30명을 넘으면 거기서 자른다.
        List<Long> referralIds = referralRepo.findFriendIds(memberId).stream()
                .filter(id -> id != null && id > 0)
                .sorted(Comparator.reverseOrder())
                .limit(LIMIT)
                .toList();

        Map<Long, MemberEntity> byId = new HashMap<>();
        if (!referralIds.isEmpty()) memberRepo.findAllById(referralIds).forEach(m -> byId.put(m.getId(), m));

        LinkedHashMap<Long, MemberEntity> result = new LinkedHashMap<>();
        for (Long id : referralIds) {
            MemberEntity m = byId.get(id);
            if (m != null) result.put(id, m);
        }
        if (result.size() < LIMIT && isDemoMember(memberId)) {
            for (MemberEntity m : memberRepo.findByEmailIn(DEMO_EMAILS)) {
                if (result.size() >= LIMIT) break;
                if (m.getId() != null && m.getId() > 0 && !m.getId().equals(memberId)) result.putIfAbsent(m.getId(), m);
            }
        }
        return List.copyOf(result.values());
    }

    /** 친구 id(순서는 {@link #friendsOf}와 같음). */
    public List<Long> friendIdsOf(Long memberId) {
        return friendsOf(memberId).stream().map(MemberEntity::getId).toList();
    }

    private boolean isDemoMember(Long memberId) {
        return memberId != null && memberRepo.findById(memberId)
                .map(m -> DemoAccounts.isDemoEmail(m.getEmail())).orElse(false);
    }
}
