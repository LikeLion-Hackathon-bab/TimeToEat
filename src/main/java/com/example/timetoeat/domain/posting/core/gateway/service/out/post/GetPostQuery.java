package com.example.timetoeat.domain.posting.core.gateway.service.out.post;

import com.example.timetoeat.domain.posting.core.gateway.service.out.data.PostData;
import com.example.timetoeat.domain.posting.domain.model.post.Post;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

import java.util.List;

public interface GetPostQuery {
    Post findById(PostId postId);
    List<PostData> findAllPosts();
}
