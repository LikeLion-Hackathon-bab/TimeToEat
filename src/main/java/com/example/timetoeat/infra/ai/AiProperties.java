package com.example.timetoeat.infra.ai;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private final String foodBaseUrl;
    private final String recoBaseUrl;
    private final String tasteBaseUrl;
    private final String outboundKey;
    private final int timeoutMillis;

    public AiProperties(String foodBaseUrl, String recoBaseUrl, String tasteBaseUrl,
                        String outboundKey, Integer timeoutMillis) {

        this.foodBaseUrl = foodBaseUrl;
        this.recoBaseUrl = recoBaseUrl;
        this.tasteBaseUrl = tasteBaseUrl;
        this.outboundKey = outboundKey;
        this.timeoutMillis = timeoutMillis == null ? 3000 : timeoutMillis;
    }

    /**
     * AI 서버로 나가는 호출에 붙일 키가 있을 때만 호출한다(보안 점검 M4).
     * 공개 기본값을 없앴으므로 AI_OUTBOUND_KEY가 비면 AI 호출을 끈다(기동은 막지 않는다).
     */
    public boolean isEnabled() {
        return outboundKey != null && !outboundKey.isBlank();
    }

    /** 꺼져 있으면 HTTP 호출 전에 실패시킨다. 호출부는 지금처럼 RuntimeException으로 처리한다(추천 503 등). */
    public void requireEnabled() {
        if (!isEnabled()) throw new AiDisabledException();
    }

    public static class AiDisabledException extends IllegalStateException {
        public AiDisabledException() { super("AI_OUTBOUND_KEY가 없어 AI 호출이 꺼져 있습니다."); }
    }
}
