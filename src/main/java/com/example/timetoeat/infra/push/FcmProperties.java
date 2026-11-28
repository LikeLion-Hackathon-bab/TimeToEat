package com.example.timetoeat.infra.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM HTTP v1 발신 설정(docs/PUSH.md).
 * serviceAccountJson 이 비면 실제 발송을 끄고 LoggingPushSender 를 쓴다.
 * tokenUri·apiBaseUrl 은 시험에서 모의 서버로 바꾸려고 둔 것이다(운영은 기본값).
 */
@ConfigurationProperties(prefix = "push.fcm")
public record FcmProperties(
        /** 서비스 계정 JSON 파일 경로(권장) 또는 base64 로 인코딩한 JSON. */
        String serviceAccountJson,
        /** OAuth2 토큰 엔드포인트. 비면 서비스 계정 JSON 의 token_uri, 그것도 없으면 Google 기본값. */
        String tokenUri,
        /** FCM API 주소. 기본 https://fcm.googleapis.com */
        String apiBaseUrl,
        /** 알림 클릭 시 열 Client 주소(예 https://babmukdang.vercel.app). 있으면 webpush.fcm_options.link 를 붙인다. */
        String linkBaseUrl,
        Integer connectTimeoutMillis,
        Integer readTimeoutMillis
) {
    public static final String DEFAULT_API_BASE_URL = "https://fcm.googleapis.com";

    public FcmProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) apiBaseUrl = DEFAULT_API_BASE_URL;
        if (connectTimeoutMillis == null) connectTimeoutMillis = 3000;
        if (readTimeoutMillis == null) readTimeoutMillis = 5000;
    }

    public boolean enabled() {
        return serviceAccountJson != null && !serviceAccountJson.isBlank();
    }
}
