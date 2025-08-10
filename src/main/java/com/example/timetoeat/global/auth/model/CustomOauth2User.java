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

    public CustomOauth2User(Oauth2UserInfo oAuth2UserInfo, Oauth2Provider oauth2Provider) {
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.attributes = oauth2Provider.getAttributes();
    }

    public static CustomOauth2User of(Oauth2UserInfo dto, Oauth2Provider oauth2Provider) {
        return new CustomOauth2User(dto, oauth2Provider);
    }

    public CustomOauth2User(Oauth2UserInfo oAuth2UserInfo) {
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.attributes = Collections.emptyMap(); // JWT에는 attributes 정보가 없으므로 비워둠
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
