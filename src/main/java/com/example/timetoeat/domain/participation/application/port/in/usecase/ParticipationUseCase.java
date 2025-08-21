package com.example.timetoeat.domain.participation.application.port.in.usecase;

import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface ParticipationUseCase {
    boolean apply(MemberId memberId, PostId postId);
}
