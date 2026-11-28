package com.example.timetoeat.domain.plans.meeting;

import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 개별 초대 방의 확정 결과. 초대당 1건(invitation_id PK). meetingAt은 Asia/Seoul 벽시계. */
@Entity
@Table(name = "confirmed_invitation_meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmedInvitationMeeting {
    @Id
    @Column(name = "invitation_id")
    private Long invitationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", insertable = false, updatable = false)
    private InvitationEntity invitation;

    @Column(name = "meeting_at", nullable = false)
    private LocalDateTime meetingAt;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String restaurant;
}
