package com.example.timetoeat.domain.post.domain.event;

import com.example.timetoeat.domain.post.domain.Post;

public record PostCompletedEvent(Post post) implements PostEvent {
    @Override
    public String getRoutingKey() {
        return "post.event.completed";
    }
}
