package com.example.timetoeat.domain.posting.core.application.port.out.gateway.participation;

import com.example.timetoeat.domain.posting.core.domain.model.participation.Participation;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public interface GetParticipationQuery {
    Participation getParticipationByPostId(PostId postId);
}
