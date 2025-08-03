package com.example.timetoeat.global.auth.model.provider;

import com.example.timetoeat.global.error.exception.CustomException;
import com.example.timetoeat.global.error.GlobalErrorCode;

import java.util.Map;

public abstract class Oauth2Provider {
    private final String registrationId;
    private final Map<String, Object> attributes;

    protected Oauth2Provider(Map<String, Object> attributes, String registrationId) {
        this.registrationId = registrationId;
        this.attributes = attributes;
    }

    // 팩토리 메서드
    public static Oauth2Provider create(Map<String, Object> attributes, String registrationId) {
        switch (registrationId) {
            case "kakao":
                return new KakaoUser(attributes,registrationId);
            default:
                throw new CustomException(GlobalErrorCode.PROVIDER_NOT_FOUND);
        }
    }

    public abstract String getEmail();
    public abstract String getUsername();
    public abstract String getProviderId();
    public abstract String getProfileUrl();
    public String getRegistrationId() {
        return registrationId;
    }
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
