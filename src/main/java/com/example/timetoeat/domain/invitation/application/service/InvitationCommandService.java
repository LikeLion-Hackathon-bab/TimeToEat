package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.application.port.in.command.SendInvitationCommand;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.InvitationUseCase;
import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitation;
import com.example.timetoeat.domain.invitation.application.port.out.PublishEventPort;
import com.example.timetoeat.domain.invitation.application.port.out.SaveInvitation;
import com.example.timetoeat.domain.invitation.domain.*;
import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationRejectedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationSentEvent;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InvitationCommandService implements InvitationUseCase {

    private final SaveInvitation saveInvitation;
    private final LoadInvitation loadInvitation;
    private final PublishEventPort publishEventPort;

    @Override
    public void sendInvitation(MemberId inviterId, SendInvitationCommand command) {
        Invitation invitation = Invitation.withoutId(
                inviterId,
                command.inviteeId(),
                command.message()
        );
        Invitation saved = saveInvitation.save(invitation);
        publishEventPort.publishEvent(new InvitationSentEvent(saved));
    }

    @Override
    public String acceptInvitation(MemberId memberId, InvitationId invitationId) {
        Invitation invitation = loadInvitation.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("not found invitation information"));

        if (!invitation.inviteeId().equals(memberId)) {
            throw new SecurityException("no permission to accept invitation");
        }

        Invitation acceptedInvitation = invitation.accept();
        Invitation savedInvitation = saveInvitation.save(acceptedInvitation);
        InvitationMatchingResult invitationMatchingResult = InvitationMatchingResult.create(savedInvitation);
        publishEventPort.publishEvent(new InvitationAcceptedEvent(invitationMatchingResult));
        return String.valueOf(savedInvitation.invitationId().id());
    }

    @Override
    public void rejectInvitation(MemberId memberId, InvitationId invitationId) {
        Invitation invitation = loadInvitation.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("not found invitation information"));

        if (!invitation.inviteeId().equals(memberId)) {
            throw new SecurityException("no permission to accept invitation");
        }

        Invitation rejectedInvitation = invitation.reject();
        Invitation savedInvitation = saveInvitation.save(rejectedInvitation);
        publishEventPort.publishEvent(new InvitationRejectedEvent(savedInvitation));
    }
}
