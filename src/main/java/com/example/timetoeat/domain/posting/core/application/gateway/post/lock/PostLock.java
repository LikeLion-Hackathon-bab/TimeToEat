package com.example.timetoeat.domain.posting.core.application.gateway.post.lock;

import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public interface PostLock {
    void lock(PostId postId);
    void unlock(PostId postId);
}
