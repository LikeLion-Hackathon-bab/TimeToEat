package com.example.timetoeat.domain.posting.core.application.gateway.postEvent;

import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;

public interface PostEventPort {
    void publish(PostEvent postEvent);
}
