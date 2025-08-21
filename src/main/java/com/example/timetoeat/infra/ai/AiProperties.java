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
}
