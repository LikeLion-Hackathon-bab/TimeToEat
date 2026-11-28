package com.example.timetoeat.infra.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 보안 점검 M4: AI_OUTBOUND_KEY 공개 기본값 제거. 비면 AI 호출을 HTTP 전에 끈다. */
class AiOutboundKeyTest {
    private AiProperties prodProps(Map<String, Object> env) throws Exception {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("env", env));
        for (var ps : new YamlPropertySourceLoader().load("prod", new ClassPathResource("application-prod.yml")))
            environment.getPropertySources().addLast(ps);
        return Binder.get(environment).bind("app.ai", AiProperties.class).get();
    }

    @Test void prodHasNoPublicDefaultKey() throws Exception {
        var props = prodProps(Map.of());
        assertTrue(props.getOutboundKey() == null || props.getOutboundKey().isBlank(), props.getOutboundKey());
        assertFalse(props.isEnabled());
        assertTrue(prodProps(Map.of("AI_OUTBOUND_KEY", "real-secret")).isEnabled());
    }

    @Test void gatewaysRefuseBeforeAnyHttpCallWhenDisabled() {
        var off = new AiProperties("http://food.invalid", "http://reco.invalid", null, "", 1000);
        // WebClient가 null이어도 NPE가 아니라 AiDisabledException: HTTP 호출 전에 막힌다
        assertThrows(AiProperties.AiDisabledException.class, () -> new HttpRecommendGateway(null, off).recommendForMember(1L, 3, 5));
        assertThrows(AiProperties.AiDisabledException.class,
                () -> new HttpAiGateway(null, off).inferFoodSync(1L, 1L, "/tmp/x.png", LocalDateTime.now()));
    }
}
