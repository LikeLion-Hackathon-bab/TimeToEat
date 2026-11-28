package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

import java.util.List;

public interface LoadPost {
    Post findById(PostId postId);
    Post findForUpdate(PostId postId);
    List<PostData> findAllPosts();
    /** status=OPEN 이고 expired_at<=now 인 공고 id, 최대 limit개. */
    List<PostId> findIdsDueForExpiry(java.time.LocalDateTime now, int limit);
}
