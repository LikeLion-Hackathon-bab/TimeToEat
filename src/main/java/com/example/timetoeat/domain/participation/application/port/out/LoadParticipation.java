package com.example.timetoeat.domain.participation.application.port.out;

import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface LoadParticipation {
    Participation getParticipationByPostId(PostId postId);
}
