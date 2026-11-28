package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * "내 친구"의 유일한 출처. 친구 목록 화면(/friends/me/meals)·태그 후보와 홈 피드(F09)가 같이 쓴다.
 * 그래서 목록에 보이는 사람 = 피드에 글이 보이는 사람.
 *
 * 친구 = 추천 코드 수령 관계(양방향, 미수령 코드 제외)를 먼저, 그다음 최근 가입 회원으로 채워 최대 {@value #LIMIT}명.
 * 나 자신과 실제 회원이 아닌 id(가짜 시연 친구 음수 id)는 들어가지 않는다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendDirectory {
    public static final int LIMIT = 30;

    private final MemberJpaRepository memberRepo;
    private final ReferralCodeRepository referralRepo;

    /** 친구 회원(우선순위 순서: 추천 관계 → 최근 가입). */
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
        for (MemberEntity m : memberRepo.findTop30ByIdNotOrderByCreatedAtDesc(memberId)) {
            if (result.size() >= LIMIT) break;
            if (m.getId() != null && m.getId() > 0) result.putIfAbsent(m.getId(), m);
        }
        return List.copyOf(result.values());
    }

    /** 친구 id(순서는 {@link #friendsOf}와 같음). */
    public List<Long> friendIdsOf(Long memberId) {
        return friendsOf(memberId).stream().map(MemberEntity::getId).toList();
    }
}
