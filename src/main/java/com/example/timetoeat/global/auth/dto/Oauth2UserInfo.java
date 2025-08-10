package com.example.timetoeat.global.auth.dto;

import com.example.timetoeat.global.auth.entity.SocialAccount;
import com.example.timetoeat.global.auth.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class Oauth2UserInfo {
    private Long memberId;
    private String username;
    private String email;
    private Role role;

    // 기존 메서드 (소셜 로그인 시 사용)
    public static Oauth2UserInfo from(SocialAccount socialAccount) {
        return Oauth2UserInfo.builder()
                .username(socialAccount.getUsername())
                .email(socialAccount.getEmail())
                .role(socialAccount.getRole())
                .memberId(socialAccount.getMemberEntity().getId())
                .build();
    }

    //  새로 추가할 메서드 (JWT 인증 시 사용)
    public static Oauth2UserInfo fromJwt(Long memberId, String username, String email, Role role) {
        return Oauth2UserInfo.builder()
                .memberId(memberId)
                .username(username)
                .email(email)
                .role(role)
                .build();
    }
}
