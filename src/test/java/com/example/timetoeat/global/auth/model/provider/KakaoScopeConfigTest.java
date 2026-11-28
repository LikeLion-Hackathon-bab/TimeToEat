package com.example.timetoeat.global.auth.model.provider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** application-prod.yml 의 kakao scope: 기본값에 account_email 이 없고, KAKAO_SCOPE 로 바꿀 수 있다. */
class KakaoScopeConfigTest {
    private Set<String> scope(Map<String, Object> env) throws Exception {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("env", env));
        for (var ps : new YamlPropertySourceLoader().load("prod", new ClassPathResource("application-prod.yml")))
            environment.getPropertySources().addLast(ps);
        var props = Binder.get(environment).bind("spring.security.oauth2.client", OAuth2ClientProperties.class).get();
        return props.getRegistration().get("kakao").getScope();
    }

    @Test void defaultScopeHasNoAccountEmail() throws Exception {
        assertEquals(Set.of("profile_nickname", "profile_image"), scope(Map.of()));
    }

    @Test void scopeCanBeOverridden() throws Exception {
        assertEquals(Set.of("profile_nickname", "profile_image", "account_email"),
                scope(Map.of("KAKAO_SCOPE", "profile_nickname,profile_image,account_email")));
    }
}
