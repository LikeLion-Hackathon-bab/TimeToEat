package com.example.timetoeat.global.auth.model.provider;

import java.util.Map;

public class KakaoUser extends Oauth2Provider {
    public KakaoUser(Map<String, Object> attributes, String registrationId) {
        super(attributes, registrationId);
    }

    @Override
    public String getProviderId() {
        return "kakao_" + getAttributes().get("id");
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        return kakaoAccount.get("email").toString();
    }

    @Override
    public String getUsername() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        Map<String, Object> profile = getProfile();
        return profile.get("nickname").toString();
    }

    @Override
    public String getProfileUrl() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        Map<String, Object> profile = getProfile();
        return profile.get("profile_image_url").toString();
    }

    private Map<String, Object> getKakaoAccount() {
        Object account = getAttributes().get("kakao_account");
        return account instanceof Map ? (Map<String, Object>) account : null;
    }

    private Map<String, Object> getProfile() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        Object profile = kakaoAccount.get("profile");
        return profile instanceof Map ? (Map<String, Object>) profile : null;
    }
}
