package com.example.timetoeat.domain.posting.application.port.out.Query;

import com.example.timetoeat.domain.posting.domain.model.Post;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public interface GetPostQuery {
    Post findById(PostId postId);
}
