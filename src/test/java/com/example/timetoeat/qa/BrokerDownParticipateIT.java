package com.example.timetoeat.qa;

import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.InetAddress;
import java.net.ServerSocket;

import static com.example.timetoeat.global.config.RabbitMqConfig.DOMAIN_EVENTS_EXCHANGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R18-1: 브로커가 응답하지 않을 때 마지막 참여(마감) POST /participate 가 브로커 연결을 기다리며 20초 넘게 멈췄다.
 * 공유 RabbitMQ 컨테이너를 멈추지 않고, TCP 연결은 받지만(커널 backlog) AMQP 응답을 절대 보내지 않는
 * '죽은 브로커' 소켓을 이 컨텍스트의 spring.rabbitmq.port 로 쓴다(정지·블랙홀 상태와 같은 무한 대기).
 */
@DirtiesContext
@AutoConfigureMockMvc
class BrokerDownParticipateIT extends QaIntegrationSupport {
    private static final ServerSocket DEAD_BROKER;
    static {
        try { DEAD_BROKER = new ServerSocket(0, 50, InetAddress.getLoopbackAddress()); } // accept 하지 않는다
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    @DynamicPropertySource
    static void deadBroker(DynamicPropertyRegistry r) {
        r.add("spring.rabbitmq.host", () -> "127.0.0.1");
        r.add("spring.rabbitmq.port", DEAD_BROKER::getLocalPort);
    }

    @AfterAll static void close() throws Exception { DEAD_BROKER.close(); }

    @Autowired MockMvc mvc;
    @MockitoSpyBean RabbitTemplate rabbit;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void lastSeatRespondsWithinThreeSecondsWhileBrokerHangs() throws Exception {
        var owner = member("모집자");
        var first = member("첫친구");
        var last = member("마지막친구");
        var p = openPost(owner, 2);
        mvc.perform(post("/api/v1/participate/" + p.getId()).with(as(first.getId()))).andExpect(status().isOk()); // MVC 예열

        long t0 = System.nanoTime();
        mvc.perform(post("/api/v1/participate/" + p.getId()).with(as(last.getId()))).andExpect(status().isOk());
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertTrue(ms < 3_000, "마감 참여 응답 " + ms + "ms (브로커 대기 때문에 늦음)");
        assertEquals(2, participationRows(p.getId()));
        assertEquals(Status.CLOSED.name(), jdbc.queryForObject("select status from post where id=?", String.class, p.getId()));
        // 발행 시도 자체는 커밋 뒤 별도 스레드에서 일어난다(실패는 로그, 대기실은 입장 API로 복구)
        verify(rabbit, timeout(10_000)).convertAndSend(eq(DOMAIN_EVENTS_EXCHANGE), eq("post.event.completed"), any(Object.class));
    }
}
