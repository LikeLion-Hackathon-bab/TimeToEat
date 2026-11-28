package com.example.timetoeat.domain.post.application.port.in.usecase;

import com.example.timetoeat.domain.post.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface PostUseCase {
    void create(MemberId memberId, CreatePostCommand command);
    String close(MemberId memberId, PostId postId);
    /**
     * 만료 시각이 지난 OPEN 공고를 마감(CLOSED)하고 PostExpiredEvent를 낸다. 멱등: 이미 마감됐거나
     * 아직 만료 전(다시 모집으로 expiredAt이 늘어난 경우 포함)이면 아무것도 하지 않고 false.
     */
    boolean expire(PostId postId);
    void delete(MemberId memberId, PostId postId);
}
