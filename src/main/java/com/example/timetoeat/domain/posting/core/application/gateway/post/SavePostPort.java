package com.example.timetoeat.domain.posting.core.application.gateway.post;

import com.example.timetoeat.domain.posting.core.domain.model.post.Post;

public interface SavePostPort {
    Post save(Post post);
}
