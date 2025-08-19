package com.example.timetoeat.domain.posting.core.application.postEvent;


import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEventType;

public interface PostEventHandler {
    boolean supports(PostEventType eventType);
    void handle(PostEvent event);
}
