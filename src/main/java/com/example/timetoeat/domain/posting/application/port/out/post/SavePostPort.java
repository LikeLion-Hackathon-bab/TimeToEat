package com.example.timetoeat.domain.posting.application.port.out.post;

import com.example.timetoeat.domain.posting.domain.model.post.Post;

public interface SavePostPort {
    Post save(Post post);
}
