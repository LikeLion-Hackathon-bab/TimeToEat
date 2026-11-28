package com.example.timetoeat.domain.invitation.adap.out.persistence;

import com.example.timetoeat.domain.invitation.application.port.out.PublishEventPort;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import com.example.timetoeat.global.event.AfterCommitPublisher;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class InvitationEventAdap implements PublishEventPort {

    private final AfterCommitPublisher publisher;

    @Override
    public void publishEvent(DomainEvent domainEvent) {
        publisher.publish(domainEvent);
    }
}
