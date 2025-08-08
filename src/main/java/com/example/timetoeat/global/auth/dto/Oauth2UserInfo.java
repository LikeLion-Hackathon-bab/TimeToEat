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

    public static Oauth2UserInfo from(SocialAccount socialAccount) {
        return Oauth2UserInfo.builder()
                .username(socialAccount.getUsername())
                .email(socialAccount.getEmail())
                .role(socialAccount.getRole())
                .memberId(socialAccount.getMemberEntity().getId())
                .build();
    }
}
