package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.domain.post.domain.PostEvent;

public interface PublishPostEvent {
    void publish(PostEvent postEvent);
}
