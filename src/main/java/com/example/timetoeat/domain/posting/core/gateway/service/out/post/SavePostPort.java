package com.example.timetoeat.domain.posting.core.gateway.service.out.post;

import com.example.timetoeat.domain.posting.domain.model.post.Post;

public interface SavePostPort {
    Post save(Post post);
}
