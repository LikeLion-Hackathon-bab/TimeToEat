package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;

import java.util.List;

public interface LoadPost {
    Post findById(PostId postId);
    List<PostData> findAllPosts();
}
