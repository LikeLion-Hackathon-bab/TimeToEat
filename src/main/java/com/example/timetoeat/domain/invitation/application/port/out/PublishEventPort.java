package com.example.timetoeat.domain.invitation.application.port.out;

import com.example.timetoeat.global.util.DomainEvent;

public interface PublishEventPort {
    void publishEvent(DomainEvent domainEvent);
}
