package com.example.timetoeat.domain.post.adap.out.persistence;

import com.example.timetoeat.domain.post.application.port.out.PublishPostEvent;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import com.example.timetoeat.global.event.AfterCommitPublisher;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class PostEventAdap implements PublishPostEvent {

    private final AfterCommitPublisher publisher;

    @Override
    public void publish(DomainEvent domainEvent) {
        publisher.publish(domainEvent);
    }
}
