package com.example.timetoeat.global.auth.model;

import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.provider.Oauth2Provider;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Getter
public class CustomOauth2User implements OAuth2User {
    private final Map<String, Object> attributes;
    private final Oauth2UserInfo oAuth2UserInfo;
    private final boolean signupRequired;

    public CustomOauth2User(Oauth2UserInfo oAuth2UserInfo, Oauth2Provider oauth2Provider) {
        this(oAuth2UserInfo, oauth2Provider, false);
    }

    public CustomOauth2User(Oauth2UserInfo oAuth2UserInfo) {
        this(oAuth2UserInfo, null, false);
    }

    // NEW: 내부 생성자
    public CustomOauth2User(Oauth2UserInfo info, Oauth2Provider provider, boolean signupRequired) {
        this.oAuth2UserInfo = info;
        this.attributes = (provider != null ? provider.getAttributes() : Collections.emptyMap());
        this.signupRequired = signupRequired;
    }

    public static CustomOauth2User of(Oauth2UserInfo dto, Oauth2Provider oauth2Provider) {
        return new CustomOauth2User(dto, oauth2Provider, false);
    }

    // NEW: 팩토리 with flag
    public static CustomOauth2User of(Oauth2UserInfo dto, Oauth2Provider provider, boolean signupRequired) {
        return new CustomOauth2User(dto, provider, signupRequired);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(oAuth2UserInfo.getRole().getKey());
        authorities.add(simpleGrantedAuthority);
        return authorities;
    }
    @Override
    public String getName() {
        return this.oAuth2UserInfo.getUsername();
    }
    public String getEmail() {
        return this.oAuth2UserInfo.getEmail();
    }
    public Role getRole() {
        return this.oAuth2UserInfo.getRole();
    }
    public Long getMemberId() {
        return this.oAuth2UserInfo.getMemberId();
    }
}
