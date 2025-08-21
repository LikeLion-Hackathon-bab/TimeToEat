package com.example.timetoeat.domain.subscription.domain;

import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public record Subscription(
        MemberId memberId,
        PostId postId
) {
    public Subscription {
        if (memberId == null || postId == null) {
            throw new IllegalArgumentException("MemberId와 PostId는 필수입니다.");
        }
    }
}
