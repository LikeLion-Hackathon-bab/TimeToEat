package com.example.timetoeat.domain.posting.core.gateway.service.out.subscription;

import com.example.timetoeat.domain.posting.domain.model.subscription.Subscription;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface SubscriptionPort {
    void save(Subscription subscription);
    boolean exists(MemberId memberId, PostId postId);
}
