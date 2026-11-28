package com.example.timetoeat.qa;

import com.example.timetoeat.domain.push.controller.PushTokenController;
import com.example.timetoeat.domain.push.dto.PushTokenRequest;
import com.example.timetoeat.domain.push.entity.PushToken;
import com.example.timetoeat.domain.push.repository.PushTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 보안 재검토 N2: 회원당 푸시 토큰은 10개까지. 넘으면 가장 오래 쓰지 않은 것부터 지운다. */
class PushTokenCapIT extends QaIntegrationSupport {
    @Autowired PushTokenController controller;
    @Autowired PushTokenRepository tokens;

    private List<String> tokensOf(long memberId) {
        return jdbc.queryForList("select token from push_token where member_id=? order by token", String.class, memberId);
    }

    @Test
    void eleventhTokenEvictsTheOldestAndReRegisteringKeepsATokenFresh() throws Exception {
        long me = member("기기많은").getId();
        long other = member("다른사람").getId();
        controller.register(other, new PushTokenRequest("other-1", "web"));
        for (int i = 1; i <= 10; i++) {
            controller.register(me, new PushTokenRequest("t" + String.format("%02d", i), "web"));
            Thread.sleep(5); // updated_at 순서가 갈리게
        }
        assertEquals(10, tokensOf(me).size());

        // t01 을 다시 등록하면 '최근'이 되어, 다음 등록 때 지워지는 것은 t02
        Thread.sleep(5);
        controller.register(me, new PushTokenRequest("t01", "web"));
        Thread.sleep(5);
        controller.register(me, new PushTokenRequest("t11", "web"));

        var left = tokensOf(me);
        assertEquals(10, left.size(), left.toString());
        assertTrue(left.contains("t01"), left.toString());
        assertTrue(left.contains("t11"), left.toString());
        assertFalse(left.contains("t02"), left.toString());
        assertEquals(List.of("other-1"), tokensOf(other), "다른 회원 토큰은 그대로");

        // 수천 개를 올려도 10개
        for (int i = 0; i < 40; i++) controller.register(me, new PushTokenRequest("bulk-" + i, "web"));
        assertEquals(10, tokensOf(me).size());
        assertTrue(tokens.findByMemberId(me).stream().map(PushToken::getToken).anyMatch("bulk-39"::equals));
    }
}
