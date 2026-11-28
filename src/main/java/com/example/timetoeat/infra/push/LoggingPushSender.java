package com.example.timetoeat.infra.push;

import com.example.timetoeat.domain.push.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** FCM 자격 증명이 없을 때의 발송기(PushSenderConfig 가 고른다): 등록된 토큰 수와 메시지를 로그로만 남긴다(FCM 미연동 환경·시험 대역). */
@Slf4j
@RequiredArgsConstructor
public class LoggingPushSender implements PushSender {

    private final PushTokenRepository tokens;

    @Override
    public void send(Long receiverId, PushMessage message) {
        int count = tokens.findByMemberId(receiverId).size();
        log.info("PUSH(dry-run) receiver={} tokens={} title={} url={}", receiverId, count, message.title(), message.url());
    }
}
