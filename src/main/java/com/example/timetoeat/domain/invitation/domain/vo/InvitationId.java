package com.example.timetoeat.domain.invitation.domain.vo;

public record InvitationId(Long id) {
    public static InvitationId of(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID는 null일 수 없습니다.");
        }
        return new InvitationId(id);
    }
}
