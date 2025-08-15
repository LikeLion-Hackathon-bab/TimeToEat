package com.example.timetoeat.domain.posting.application.port.out.postEvent;

import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;

public interface PostEventPort {
    void publish(PostEvent postEvent);
}
