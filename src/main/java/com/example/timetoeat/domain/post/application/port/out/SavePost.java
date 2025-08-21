package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.domain.post.domain.Post;

public interface SavePost {
    Post save(Post post);
}
