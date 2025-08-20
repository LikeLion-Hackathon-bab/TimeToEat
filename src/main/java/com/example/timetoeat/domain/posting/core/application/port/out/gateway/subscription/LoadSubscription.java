package com.example.timetoeat.domain.posting.core.application.port.out.gateway.subscription;

import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

import java.util.List;

public interface LoadSubscription {
    List<MemberId> findMembersByPostId(PostId postId);
}
