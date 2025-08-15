package com.example.timetoeat.domain.posting.domain.model;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class Subscription {
    private final MemberId memberId;
    private final PostId postId;

    public static Subscription create(MemberId memberId, PostId postId) {
        if (memberId == null || postId == null) {
            throw new IllegalArgumentException("MemberId와 PostId는 필수입니다.");
        }
        return new Subscription(memberId, postId);
    }

    private Subscription(MemberId memberId, PostId postId) {
        this.memberId = memberId;
        this.postId = postId;
    }
}
