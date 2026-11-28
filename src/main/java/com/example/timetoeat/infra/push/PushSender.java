package com.example.timetoeat.infra.push;

/**
 * 웹 푸시 발송 포트(기획 문서 §15.4 infra.push).
 * 구현: FcmHttpV1PushSender(FCM_SERVICE_ACCOUNT_JSON 설정 시), LoggingPushSender(미설정 시). 선택은 PushSenderConfig, 문서 docs/PUSH.md.
 */
public interface PushSender {
    void send(Long receiverId, PushMessage message);
}
