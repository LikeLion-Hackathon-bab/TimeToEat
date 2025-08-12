package com.example.timetoeat.domain.posting.application.port.out.lock;

import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface PostLock {
    void lock(PostId postId);
    void unlock(PostId postId);
}
