package com.example.timetoeat.infra.push;

/** 웹 푸시 본문. url 은 알림 클릭 시 열 Client 경로(Deep Link, 기획 문서 §15.2). */
public record PushMessage(String title, String body, String url) {
}
