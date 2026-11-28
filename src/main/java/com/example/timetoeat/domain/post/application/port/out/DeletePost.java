package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface DeletePost {
    boolean hasParticipants(PostId postId);
    void delete(PostId postId);
}
