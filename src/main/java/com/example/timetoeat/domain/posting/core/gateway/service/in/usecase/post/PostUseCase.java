package com.example.timetoeat.domain.posting.core.gateway.service.in.usecase.post;

import com.example.timetoeat.domain.posting.core.gateway.service.in.command.CreatePostCommand;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface PostUseCase {
    void create(MemberId memberId, CreatePostCommand command);
    void close(MemberId memberId, PostId postId);
    void expire(PostId postId);
}
