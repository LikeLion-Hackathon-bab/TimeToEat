package com.example.timetoeat.domain.post.application.port.in.usecase;

import com.example.timetoeat.domain.post.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface PostUseCase {
    void create(MemberId memberId, CreatePostCommand command);
    void close(MemberId memberId, PostId postId);
    void expire(PostId postId);
}
