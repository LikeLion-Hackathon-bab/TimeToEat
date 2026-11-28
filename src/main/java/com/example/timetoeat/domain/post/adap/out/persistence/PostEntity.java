package com.example.timetoeat.domain.post.adap.out.persistence;

import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "meeting_at", nullable = false)
    private LocalDateTime meetingAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, length = 100)
    private String message;

    /** 마감 사유(CloseReason). OPEN이면 null. 기존 DB는 db/post_close_reason.sql 로 컬럼을 더한다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason")
    private com.example.timetoeat.domain.post.domain.CloseReason closeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setCloseReason(com.example.timetoeat.domain.post.domain.CloseReason closeReason) {
        this.closeReason = closeReason;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}
