package com.example.timetoeat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TimeToEatApplication {

    public static void main(String[] args) {
        // 모든 LocalDateTime(감사 필드 created_at 포함)을 한국 벽시계로 통일한다.
        // 운영 이미지(eclipse-temurin)는 기본 UTC라 created_at만 9시간 어긋났다. 계약: qa-cloud/contracts/time-format.md
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(TimeToEatApplication.class, args);
    }

}
