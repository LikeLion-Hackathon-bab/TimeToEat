package com.example.timetoeat.domain.subscription.application.port.in.usecase;

import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface SubscriptionUseCase {
    void subscribe(MemberId memberId, PostId postId);
}
