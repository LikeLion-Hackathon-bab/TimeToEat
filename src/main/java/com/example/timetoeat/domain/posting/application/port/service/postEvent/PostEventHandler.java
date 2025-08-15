package com.example.timetoeat.domain.posting.application.port.service.postEvent;


import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEventType;

public interface PostEventHandler {
    boolean supports(PostEventType eventType);
    void handle(PostEvent event);
}
