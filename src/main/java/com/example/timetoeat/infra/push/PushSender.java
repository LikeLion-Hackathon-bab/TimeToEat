package com.example.timetoeat.infra.push;

/**
 * 웹 푸시 발송 포트(기획 문서 §15.4 infra.push).
 * 실제 FCM HTTP v1 구현은 Firebase 프로젝트·서비스 계정이 필요해 이 패치에 넣지 않았다(외부 공급자 미검증).
 */
public interface PushSender {
    void send(Long receiverId, PushMessage message);
}
