package com.example.timetoeat.domain.posting.core.domain.model.postEvent;

import com.example.timetoeat.domain.posting.core.domain.model.post.Post;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;

public record PostEvent(
    PostId postId,
    PostEventType eventType
){

    public PostEvent {
        if (postId == null || eventType == null) {
            throw new IllegalArgumentException("PostId와 eventType은 null일 수 없습니다");
        }
    }

    public static PostEvent create(Post post, PostEventType eventType) {
        return new PostEvent(post.getPostId(), eventType);
    }

    public String getRoutingKey() {
        return "post.event." + this.eventType.name().toLowerCase();
    }
}
