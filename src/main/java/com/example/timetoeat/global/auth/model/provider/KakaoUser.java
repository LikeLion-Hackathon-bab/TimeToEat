package com.example.timetoeat.global.auth.model.provider;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * 카카오 사용자 정보(/v2/user/me).
 * 동의 항목은 앱 설정에 따라 빠질 수 있다. 새 앱(1670764)은 account_email이 "권한 없음"이라
 * kakao_account.email이 오지 않는다. 그래서 어떤 필드도 있다고 가정하지 않는다.
 * - email이 없으면 카카오 회원번호로 만든 고정 합성 주소를 쓴다(member.email NOT NULL UNIQUE).
 *   같은 사람은 매번 같은 값이고, 사람마다 다르다.
 * - nickname이 없으면 properties.nickname, 그것도 없으면 기본 이름.
 * - 프로필 이미지가 없으면 null.
 */
public class KakaoUser extends Oauth2Provider {
    /** 합성 이메일 도메인. 실제로 메일을 받지 않는 주소임을 보이려고 noreply를 붙였다. */
    public static final String SYNTHETIC_EMAIL_DOMAIN = "users.noreply.babmuckdang";
    static final String DEFAULT_NAME = "밥먹댕 친구";

    public KakaoUser(Map<String, Object> attributes, String registrationId) {
        super(attributes, registrationId);
    }

    /** 합성 이메일인지(실제 주소가 아님). 화면·응답에 이메일을 내보낼 일이 생기면 이것으로 걸러 낸다. */
    public static boolean isSyntheticEmail(String email) {
        return email != null && email.endsWith("@" + SYNTHETIC_EMAIL_DOMAIN);
    }

    @Override
    public String getProviderId() {
        return "kakao_" + kakaoId();
    }

    @Override
    public String getEmail() {
        String email = text(getKakaoAccount(), "email");
        return email != null ? email : "kakao_" + kakaoId() + "@" + SYNTHETIC_EMAIL_DOMAIN;
    }

    @Override
    public String getUsername() {
        String name = text(getProfile(), "nickname");
        if (name == null) name = text(getProperties(), "nickname");
        return name != null ? name : DEFAULT_NAME;
    }

    @Override
    public String getProfileUrl() {
        String url = text(getProfile(), "profile_image_url");
        return url != null ? url : text(getProperties(), "profile_image");
    }

    private String kakaoId() {
        Object id = getAttributes() == null ? null : getAttributes().get("id");
        if (id == null || id.toString().isBlank())
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info", "카카오 회원번호(id)가 없습니다.", null));
        return id.toString();
    }

    private Map<String, Object> getKakaoAccount() {
        return map(getAttributes(), "kakao_account");
    }

    private Map<String, Object> getProfile() {
        return map(getKakaoAccount(), "profile");
    }

    private Map<String, Object> getProperties() {
        return map(getAttributes(), "properties");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> from, String key) {
        Object v = from == null ? null : from.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private static String text(Map<String, Object> from, String key) {
        Object v = from == null ? null : from.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
