package com.example.timetoeat.domain.posting.core.application.port.out.gateway.post;

import com.example.timetoeat.domain.posting.core.domain.model.post.Post;

public interface SavePostPort {
    Post save(Post post);
}
