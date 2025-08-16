package com.example.timetoeat.global.auth.entity;

import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
@Entity
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Date expiredAt;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean revoked;

    public static RefreshToken create(Long memberId, String token, Date expiredAt) {
        return RefreshToken.builder()
                .memberId(memberId)
                .token(token)
                .expiredAt(expiredAt)
                .build();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiredAt.toInstant());
    }

    // 토큰이 강제 무효화 되었는지 여부
    public void revoke() {
        this.revoked = true;
    }

    public void update(String token, Date expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }
}
