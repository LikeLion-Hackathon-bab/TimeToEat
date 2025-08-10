package com.example.timetoeat.global.auth.entity;

import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.auth.model.provider.Oauth2Provider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Table(name = "social_account")
@Entity
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private MemberEntity memberEntity;

    @Column(name = "registration_id", nullable = false, length = 50)
    private String registrationId;  // 예: google, kakao, naver, facebook

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;  // 소셜 로그인 제공자가 부여한 고유 ID

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_url", length = 512)
    private String profileUrl;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "role", nullable = false)
    private Role role;

    public static SocialAccount from(Oauth2Provider oauth2Provider, MemberEntity memberEntity) {
        return SocialAccount.builder()
                .email(oauth2Provider.getEmail())
                .registrationId(oauth2Provider.getRegistrationId())
                .providerId(oauth2Provider.getProviderId())
                .profileUrl(oauth2Provider.getProfileUrl())
                .username(oauth2Provider.getUsername()) // 필요하다면
                .role(memberEntity.getRole())
                .memberEntity(memberEntity)
                .build();
    }
}
