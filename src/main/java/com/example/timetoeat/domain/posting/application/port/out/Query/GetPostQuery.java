package com.example.timetoeat.domain.posting.application.port.out.Query;

import com.example.timetoeat.domain.posting.application.port.out.data.PostData;
import com.example.timetoeat.domain.posting.domain.model.Post;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

import java.util.List;

public interface GetPostQuery {
    Post findById(PostId postId);
    List<PostData> findAllPosts();
}
