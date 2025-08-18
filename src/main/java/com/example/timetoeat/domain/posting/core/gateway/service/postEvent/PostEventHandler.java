package com.example.timetoeat.domain.posting.core.gateway.service.postEvent;


import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEventType;

public interface PostEventHandler {
    boolean supports(PostEventType eventType);
    void handle(PostEvent event);
}
