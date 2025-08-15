package com.example.timetoeat.domain.posting.application.port.in.usecase;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface SubscriptionUseCase {
    void subscribe(MemberId memberId, PostId postId);
}
