package com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.participation;

import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public interface ParticipationUseCase {
    boolean apply(MemberId memberId, PostId postId);
}
