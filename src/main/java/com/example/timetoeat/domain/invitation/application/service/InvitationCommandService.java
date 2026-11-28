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
import com.example.timetoeat.domain.invitation.exception.InvitationErrorCode;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.error.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
        // F40: 더블클릭·동시 요청에도 같은 보낸이→받는이 PENDING 초대는 1건만. 쌍 잠금 뒤 확인해야 경합을 막는다.
        loadInvitation.lockPair(inviterId, command.inviteeId());
        if (loadInvitation.existsPending(inviterId, command.inviteeId())) {
            throw new CustomException(InvitationErrorCode.ALREADY_PENDING);
        }
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
        Invitation invitation = pendingForRecipient(memberId, invitationId);

        Invitation acceptedInvitation = invitation.accept();
        Invitation savedInvitation = saveInvitation.save(acceptedInvitation);
        InvitationMatchingResult invitationMatchingResult = InvitationMatchingResult.create(savedInvitation);
        publishEventPort.publishEvent(new InvitationAcceptedEvent(invitationMatchingResult));
        return String.valueOf(savedInvitation.invitationId().id());
    }

    @Override
    public void rejectInvitation(MemberId memberId, InvitationId invitationId) {
        Invitation invitation = pendingForRecipient(memberId, invitationId);

        Invitation rejectedInvitation = invitation.reject();
        Invitation savedInvitation = saveInvitation.save(rejectedInvitation);
        publishEventPort.publishEvent(new InvitationRejectedEvent(savedInvitation));
    }
    private Invitation pendingForRecipient(MemberId viewer, InvitationId id) {
        var invitation = loadInvitation.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!invitation.inviteeId().equals(viewer)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (invitation.status() != InvitationStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 초대입니다.");
        return invitation;
    }
}
