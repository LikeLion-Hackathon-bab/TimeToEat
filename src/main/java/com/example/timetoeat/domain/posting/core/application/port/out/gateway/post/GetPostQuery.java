package com.example.timetoeat.domain.posting.core.application.port.out.gateway.post;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.data.PostData;
import com.example.timetoeat.domain.posting.core.domain.model.post.Post;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

import java.util.List;

public interface GetPostQuery {
    Post findById(PostId postId);
    List<PostData> findAllPosts();
}
