package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.domain.event.PostCompletedEvent;
import com.example.timetoeat.domain.subscription.application.port.out.LoadSubscription;
import com.example.timetoeat.domain.post.domain.PostEventType;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.common.EventHandler;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompletedEventHandler implements EventHandler<PostCompletedEvent> {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PostCompletedEvent;
    }

    @Override
    public void handle(PostCompletedEvent event) {

    }
}
