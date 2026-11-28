package com.example.timetoeat.infra.push;

import com.example.timetoeat.domain.push.repository.PushTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 발송기 선택: FCM_SERVICE_ACCOUNT_JSON(push.fcm.service-account-json)이 있으면 FCM HTTP v1, 없으면 로그만 남기는 대역.
 * 값이 있는데 읽을 수 없으면 기동을 실패시킨다(운영에서 푸시가 조용히 꺼진 채 배포되지 않게).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class PushSenderConfig {

    @Bean
    public PushSender pushSender(FcmProperties props, PushTokenRepository tokens) {
        if (!props.enabled()) {
            log.info("웹 푸시: FCM_SERVICE_ACCOUNT_JSON 없음 → LoggingPushSender(dry-run)");
            return new LoggingPushSender(tokens);
        }
        ObjectMapper om = new ObjectMapper();
        FcmServiceAccount account = FcmServiceAccount.load(props.serviceAccountJson(), om);
        log.info("웹 푸시: FCM HTTP v1 project={}", account.projectId());
        return new FcmHttpV1PushSender(account, props, tokens, om, Clock.systemUTC());
    }
}
