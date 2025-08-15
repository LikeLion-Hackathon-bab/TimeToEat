package com.example.timetoeat.domain.posting.application.port.out.participation;

import com.example.timetoeat.domain.posting.domain.model.participation.Participation;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface GetParticipationQuery {
    Participation getParticipationByPostId(PostId postId);
}
