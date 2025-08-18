package com.example.timetoeat.domain.posting.core.gateway.service.out.postEvent;

import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;

public interface PostEventPort {
    void publish(PostEvent postEvent);
}
