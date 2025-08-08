package com.example.timetoeat.domain.posting.domain.model;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Participation {

    @Getter private final PostId postId;
    @Getter private final Set<MemberId> memberIds;

    public Participation add(MemberId memberId) {
        Set<MemberId> updated = new HashSet<>(memberIds);
        updated.add(memberId);
        return new Participation(postId, Collections.unmodifiableSet(updated));
    }

    public static Participation empty(PostId postId) {
        return new Participation(postId, Collections.emptySet());
    }

    public boolean contains(MemberId memberId) {
        return memberIds.contains(memberId);
    }

    public boolean isFull(int targetCount) {
        return memberIds.size() >= targetCount;
    }
}
