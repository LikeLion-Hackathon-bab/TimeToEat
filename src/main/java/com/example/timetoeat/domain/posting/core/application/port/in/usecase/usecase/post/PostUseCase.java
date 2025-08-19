package com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.post;

import com.example.timetoeat.domain.posting.core.application.port.in.usecase.command.CreatePostCommand;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public interface PostUseCase {
    void create(MemberId memberId, CreatePostCommand command);
    void close(MemberId memberId, PostId postId);
    void expire(PostId postId);
}
