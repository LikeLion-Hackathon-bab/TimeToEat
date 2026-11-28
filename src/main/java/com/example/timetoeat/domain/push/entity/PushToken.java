package com.example.timetoeat.domain.push.entity;

import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** FCM 등록 토큰(기기·브라우저별). 기획 문서 §15.2 표: FCM 은 endpoint/p256dh/auth 대신 등록 토큰을 저장. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "push_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_push_token_token", columnNames = "token"),
        indexes = @Index(name = "idx_push_token_member", columnList = "member_id"))
public class PushToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "platform", length = 32)
    private String platform;

    public PushToken(Long memberId, String token, String platform) {
        this.memberId = memberId;
        this.token = token;
        this.platform = platform;
    }

    /** 같은 브라우저에서 다른 계정으로 로그인하면 토큰 소유자를 옮긴다. */
    public void reassign(Long memberId, String platform) {
        this.memberId = memberId;
        this.platform = platform;
    }
}
