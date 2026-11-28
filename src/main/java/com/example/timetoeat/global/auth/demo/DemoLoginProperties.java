package com.example.timetoeat.global.auth.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 시연 로그인 설정. DEMO_LOGIN_ENABLED=true 일 때만 빈이 생긴다(기본 꺼짐 → 경로 404).
 * 켰는데 비밀값이 32자 미만이면 기동을 멈춘다(잘못 켠 채로 운영되지 않게). 기한은 {@link DemoLoginWindow}.
 */
@Component
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoLoginProperties {
    static final int MIN_SECRET_LENGTH = 32;
    public static final int ACCOUNT_COUNT = 5;

    private final byte[] secret;
    private final DemoLoginWindow window;

    @org.springframework.beans.factory.annotation.Autowired
    public DemoLoginProperties(@Value("${demo-login.secret:}") String secret, DemoLoginWindow window) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("DEMO_LOGIN_ENABLED=true 이면 DEMO_LOGIN_SECRET(" + MIN_SECRET_LENGTH + "자 이상)이 필요합니다.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.window = window;
    }

    byte[] secret() { return secret.clone(); }

    /** 기한이 지났거나 기한 형식이 틀렸으면 true. */
    boolean expired() {
        return !window.isOpen();
    }
}
