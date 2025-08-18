package com.example.timetoeat.domain.posting.core.gateway.service.out.post.lock;

import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface PostLock {
    void lock(PostId postId);
    void unlock(PostId postId);
}
