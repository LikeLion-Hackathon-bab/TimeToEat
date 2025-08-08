package com.example.timetoeat.domain.posting.application.port.in.usecase;

import com.example.timetoeat.global.auth.entity.Member;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.domain.posting.dto.request.PostReq;

public interface PostUseCase {
    void create(Member member, PostReq postReq);
    void close(PostId postId);
    void expire(PostId postId);
}
