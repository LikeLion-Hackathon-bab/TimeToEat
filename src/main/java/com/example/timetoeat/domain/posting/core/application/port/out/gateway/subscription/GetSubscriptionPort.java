package com.example.timetoeat.domain.posting.core.application.port.out.gateway.subscription;

import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

import java.util.List;

public interface GetSubscriptionPort {
    List<MemberId> findMembersByPostId(PostId postId);
}
