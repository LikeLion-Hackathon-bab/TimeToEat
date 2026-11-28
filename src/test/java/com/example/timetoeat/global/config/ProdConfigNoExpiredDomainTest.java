package com.example.timetoeat.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** 재리뷰 L: 운영 설정에 만료 도메인(babmuckdang.site)이 남지 않는다. springdoc.server-url은 읽는 코드가 없는 죽은 설정이라 지웠다. */
class ProdConfigNoExpiredDomainTest {
    @Test void prodYamlHasNoExpiredDomain() throws Exception {
        for (String f : new String[]{"application-prod.yml", "application.yml"}) {
            String yml = new String(new ClassPathResource(f).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(yml.contains("babmuckdang.site"), f);
        }
    }
}
