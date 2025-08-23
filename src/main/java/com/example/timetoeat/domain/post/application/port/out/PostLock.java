package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.domain.post.domain.vo.post.PostId;

public interface PostLock {
    void lock(PostId postId);
    void unlock(PostId postId);
}
