package com.example.timetoeat.domain.posting.adapter.out.persistence.post;

import com.example.timetoeat.domain.posting.domain.model.post.Status;
import com.example.timetoeat.global.auth.entity.MemberEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}
