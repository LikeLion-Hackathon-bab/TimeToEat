package com.example.timetoeat.global.auth.service;

import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.domain.member.entity.SocialAccount;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.provider.Oauth2Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class CustomOauthUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest); // 인가 서버와 통신해서 실제 사용자 정보 조회
        Oauth2Provider oauth2Provider = this.getOauth2ProviderUsers(clientRegistration, oAuth2User);

        SocialAccount socialAccount = memberService.findByProviderId(oauth2Provider);
        log.info("소셜 로그인 - 유저 아이디: {}, 유저명: {}, 이메일: {},", socialAccount.getProviderId(), socialAccount.getUsername(), socialAccount.getEmail());

        Oauth2UserInfo dto = Oauth2UserInfo.from(socialAccount);
        return CustomOauth2User.of(dto, oauth2Provider);
    }

    private Oauth2Provider getOauth2ProviderUsers(ClientRegistration clientRegistration, OAuth2User oAuth2User) {
        String registrationId = clientRegistration.getRegistrationId(); //kakao, google, naver
        Map<String, Object> attributes = oAuth2User.getAttributes();
        return Oauth2Provider.create(attributes, registrationId);
    }
}
