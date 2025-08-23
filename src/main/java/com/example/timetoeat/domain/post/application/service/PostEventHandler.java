package com.example.timetoeat.domain.post.application.service;


import com.example.timetoeat.domain.post.domain.PostEvent;
import com.example.timetoeat.domain.post.domain.PostEventType;

public interface PostEventHandler {
    boolean supports(PostEventType eventType);
    void handle(PostEvent event);
}
