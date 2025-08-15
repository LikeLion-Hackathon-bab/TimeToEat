package com.example.timetoeat.domain.posting.application.port.out.save;

import com.example.timetoeat.domain.posting.domain.model.Subscription;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface SubscriptionPort {
    void save(Subscription subscription);
    boolean exists(MemberId memberId, PostId postId);
}
