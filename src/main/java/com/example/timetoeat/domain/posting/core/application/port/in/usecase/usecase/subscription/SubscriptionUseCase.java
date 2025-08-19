package com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.subscription;

import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public interface SubscriptionUseCase {
    void subscribe(MemberId memberId, PostId postId);
}
