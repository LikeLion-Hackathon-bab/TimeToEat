package com.example.timetoeat.domain.posting.core.application.gateway.subscription;

import com.example.timetoeat.domain.posting.core.domain.model.subscription.Subscription;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public interface SubscriptionPort {
    void save(Subscription subscription);
    boolean exists(MemberId memberId, PostId postId);
}
