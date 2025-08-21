package com.example.timetoeat.domain.subscription.application.port.out;

import com.example.timetoeat.domain.subscription.domain.Subscription;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface SaveSubscription {
    void save(Subscription subscription);
    boolean exists(MemberId memberId, PostId postId);
}
