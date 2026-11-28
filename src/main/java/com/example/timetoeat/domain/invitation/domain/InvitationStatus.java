package com.example.timetoeat.domain.invitation.domain;

public enum InvitationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** 수락 뒤 대기실에서 한쪽이 나감(F46). */
    CANCELLED
}
