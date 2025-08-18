package com.example.timetoeat.domain.posting.core.gateway.service.out.subscription;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

import java.util.List;

public interface GetSubscriptionPort {
    List<MemberId> findMembersByPostId(PostId postId);
}
