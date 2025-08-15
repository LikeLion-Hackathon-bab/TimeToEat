package com.example.timetoeat.domain.posting.application.port.in.usecase.participation;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface ParticipationUseCase {
    boolean apply(MemberId memberId, PostId postId);
}
