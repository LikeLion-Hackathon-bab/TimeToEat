package com.example.timetoeat.domain.posting.application.port.out.Query;

import com.example.timetoeat.domain.posting.domain.model.Participation;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface GetParticipationQuery {
    Participation getParticipationByPostId(PostId postId);
}
