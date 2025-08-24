package com.example.timetoeat.domain.post.application.port.out;

import com.example.timetoeat.global.util.DomainEvent;

public interface PublishPostEvent {
    void publish(DomainEvent domainEvent);
}
