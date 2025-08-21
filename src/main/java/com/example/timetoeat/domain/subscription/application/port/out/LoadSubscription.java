package com.example.timetoeat.domain.subscription.application.port.out;

import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

import java.util.List;

public interface LoadSubscription {
    List<MemberId> findMembersByPostId(PostId postId);
}
