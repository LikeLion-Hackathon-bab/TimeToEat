package com.example.timetoeat.domain.invitation.domain;

import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;

import java.time.LocalDateTime;

public record Invitation(
    InvitationId invitationId,
    MemberId inviterId,
    MemberId inviteeId,
    String message,
    InvitationStatus status,
    LocalDateTime createdAt
) {
    public Invitation{
        if (inviterId.equals(inviteeId)) {
            throw new IllegalArgumentException("Invitation id is the same");
        }
    }

    public static Invitation withoutId(
        MemberId inviterId,
        MemberId inviteeId,
        String message
    ) {
        return new Invitation(
                null,
                inviterId,
                inviteeId,
                message,
                InvitationStatus.PENDING,
                LocalDateTime.now()
        );
    }

    public static Invitation withId(
        InvitationId invitationId,
        MemberId inviterId,
        MemberId inviteeId,
        String message,
        InvitationStatus status,
        LocalDateTime createdAt
    ) {
        return new Invitation(
                invitationId,
                inviterId,
                inviteeId,
                message,
                status,
                createdAt
        );
    }

    public Invitation accept() {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException("already processed");
        }
        return new Invitation(
                this.invitationId,
                this.inviterId,
                this.inviteeId,
                this.message,
                InvitationStatus.APPROVED,
                this.createdAt
        );
    }

    public Invitation reject() {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException("already processed");
        }
        return new Invitation(
                this.invitationId,
                this.inviterId,
                this.inviteeId,
                this.message,
                InvitationStatus.REJECTED,
                this.createdAt
        );
    }
}
