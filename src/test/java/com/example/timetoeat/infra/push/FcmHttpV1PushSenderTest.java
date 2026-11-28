package com.example.timetoeat.infra.push;

import com.example.timetoeat.domain.push.entity.PushToken;
import com.example.timetoeat.domain.push.repository.PushTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** F87: FCM HTTP v1 발송기. 모의 HTTP 서버(com.sun.net.httpserver)로만 검증한다(실제 Google 호출 없음). */
class FcmHttpV1PushSenderTest {

    record Req(String method, String path, String auth, String body) { }

    private static final KeyPair KEYS;
    static {
        try {
            var g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            KEYS = g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private final ObjectMapper om = new ObjectMapper();
    private HttpServer server;
    private String base;
    private final List<Req> requests = new CopyOnWriteArrayList<>();
    /** messages:send 응답(토큰 값 → [status, body]). 없으면 200. */
    private final Map<String, Object[]> sendReplies = new HashMap<>();
    private int tokenCalls;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", ex -> {
            String body = read(ex);
            requests.add(new Req(ex.getRequestMethod(), ex.getRequestURI().getPath(), null, body));
            tokenCalls++;
            reply(ex, 200, "{\"access_token\":\"ya29.mock\",\"expires_in\":3599,\"token_type\":\"Bearer\"}");
        });
        server.createContext("/v1/", ex -> {
            String body = read(ex);
            requests.add(new Req(ex.getRequestMethod(), ex.getRequestURI().getPath(),
                    ex.getRequestHeaders().getFirst("Authorization"), body));
            String token = new ObjectMapper().readTree(body).path("message").path("token").asText();
            Object[] r = sendReplies.getOrDefault(token, new Object[]{200, "{\"name\":\"projects/p/messages/1\"}"});
            reply(ex, (Integer) r[0], (String) r[1]);
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static String read(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    private String serviceAccountJson() throws Exception {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(KEYS.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Map<String, String> sa = new LinkedHashMap<>();
        sa.put("type", "service_account");
        sa.put("project_id", "test-proj");
        sa.put("private_key_id", "kid");
        sa.put("private_key", pem);
        sa.put("client_email", "fcm@test-proj.iam.gserviceaccount.com");
        sa.put("token_uri", "https://oauth2.googleapis.com/token");
        return om.writeValueAsString(sa);
    }

    private FcmProperties props(String saValue, String linkBase) {
        return new FcmProperties(saValue, base + "/token", base, linkBase, 1000, 2000);
    }

    private static PushToken token(long id, long memberId, String value) {
        PushToken t = mock(PushToken.class);
        when(t.getId()).thenReturn(id);
        when(t.getMemberId()).thenReturn(memberId);
        when(t.getToken()).thenReturn(value);
        return t;
    }

    private FcmHttpV1PushSender sender(PushTokenRepository repo, String linkBase) throws Exception {
        var p = props(serviceAccountJson(), linkBase);
        return new FcmHttpV1PushSender(FcmServiceAccount.load(p.serviceAccountJson(), om), p, repo, om, Clock.systemUTC());
    }

    @Test
    void sendsWithSignedJwtAccessTokenAndHttpV1Body() throws Exception {
        PushTokenRepository repo = mock(PushTokenRepository.class);
        List<PushToken> two = List.of(token(1, 7, "tok-a"), token(2, 7, "tok-b"));
        when(repo.findByMemberId(7L)).thenReturn(two);
        var s = sender(repo, "https://babmukdang.example");

        s.send(7L, new PushMessage("약속 확정", "오늘 12시", "/meetings/3"));
        s.send(7L, new PushMessage("두 번째", null, null));

        // 토큰 엔드포인트: jwt-bearer, RS256 서명, scope·aud·iss
        assertEquals(1, tokenCalls, "access token 은 만료 전까지 재사용");
        Req tokenReq = requests.get(0);
        assertEquals("/token", tokenReq.path());
        Map<String, String> form = new HashMap<>();
        for (String kv : tokenReq.body().split("&")) {
            String[] p = kv.split("=", 2);
            form.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p[1], StandardCharsets.UTF_8));
        }
        assertEquals("urn:ietf:params:oauth:grant-type:jwt-bearer", form.get("grant_type"));
        Claims claims = Jwts.parser().verifyWith(KEYS.getPublic()).build().parseSignedClaims(form.get("assertion")).getPayload();
        assertEquals("fcm@test-proj.iam.gserviceaccount.com", claims.getIssuer());
        assertEquals(FcmHttpV1PushSender.SCOPE, claims.get("scope"));
        assertThat(claims.getAudience()).containsExactly(base + "/token");
        String payloadJson = new String(Base64.getUrlDecoder().decode(form.get("assertion").split("\\.")[1]), StandardCharsets.UTF_8);
        assertTrue(om.readTree(payloadJson).path("aud").isTextual(), "aud 는 배열이 아닌 문자열: " + payloadJson);

        // messages:send
        List<Req> sends = requests.stream().filter(r -> r.path().startsWith("/v1/")).toList();
        assertEquals(4, sends.size());
        for (Req r : sends) {
            assertEquals("POST", r.method());
            assertEquals("/v1/projects/test-proj/messages:send", r.path());
            assertEquals("Bearer ya29.mock", r.auth());
        }
        JsonNode m = om.readTree(sends.get(0).body()).path("message");
        assertEquals("tok-a", m.path("token").asText());
        assertEquals("약속 확정", m.path("notification").path("title").asText());
        assertEquals("오늘 12시", m.path("notification").path("body").asText());
        assertEquals("/meetings/3", m.path("data").path("url").asText());
        assertEquals("https://babmukdang.example/meetings/3", m.path("webpush").path("fcm_options").path("link").asText());
        JsonNode m2 = om.readTree(sends.get(3).body()).path("message");
        assertEquals("tok-b", m2.path("token").asText());
        assertFalse(m2.has("data"));
        assertFalse(m2.path("notification").has("body"));
        verify(repo, never()).delete(any());
    }

    @Test
    void relativeUrlWithoutLinkBaseHasNoFcmOptionsLink() throws Exception {
        var s = sender(mock(PushTokenRepository.class), null);
        Map<String, Object> msg = s.buildMessage("t", new PushMessage("a", "b", "/x"));
        assertFalse(msg.containsKey("webpush"), "상대 경로 link 는 FCM 이 400 으로 거부한다");
        assertEquals(Map.of("url", "/x"), msg.get("data"));
    }

    @Test
    void unregisteredAndInvalidTokenRowsAreDeletedButServerErrorsOnlyLogged() throws Exception {
        PushTokenRepository repo = mock(PushTokenRepository.class);
        PushToken gone = token(1, 7, "tok-gone");
        PushToken bad = token(2, 7, "tok-bad");
        PushToken busy = token(3, 7, "tok-busy");
        PushToken down = token(4, 7, "tok-down");
        PushToken ok = token(5, 7, "tok-ok");
        when(repo.findByMemberId(7L)).thenReturn(List.of(gone, bad, busy, down, ok));
        when(repo.findByToken("tok-gone")).thenReturn(Optional.of(gone));
        when(repo.findByToken("tok-bad")).thenReturn(Optional.of(bad));
        sendReplies.put("tok-gone", new Object[]{404, """
                {"error":{"code":404,"message":"Requested entity was not found.","status":"NOT_FOUND",
                 "details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"UNREGISTERED"}]}}"""});
        sendReplies.put("tok-bad", new Object[]{400, """
                {"error":{"code":400,"message":"The registration token is not a valid FCM registration token","status":"INVALID_ARGUMENT",
                 "details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"INVALID_ARGUMENT"}]}}"""});
        sendReplies.put("tok-busy", new Object[]{429, "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}"});
        sendReplies.put("tok-down", new Object[]{503, "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}"});

        sender(repo, null).send(7L, new PushMessage("t", "b", "/"));

        verify(repo).delete(gone);
        verify(repo).delete(bad);
        verify(repo, times(2)).delete(any());
        verify(repo, never()).findByToken("tok-busy");
        // 429·5xx 는 재시도하지 않는다: 토큰마다 정확히 1번
        assertEquals(5, requests.stream().filter(r -> r.path().startsWith("/v1/")).count());
    }

    @Test
    void doesNotDeleteTokensOnNonTokenErrors() throws Exception {
        var s = sender(mock(PushTokenRepository.class), null);
        // 잘못된 project_id 의 404(UNREGISTERED 아님), 본문 형식 오류 400 은 토큰과 무관
        assertFalse(s.isInvalidToken(404, "{\"error\":{\"code\":404,\"status\":\"NOT_FOUND\",\"message\":\"Requested entity was not found.\"}}"));
        assertFalse(s.isInvalidToken(400, "{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\",\"message\":\"Invalid JSON payload received.\"}}"));
        assertTrue(s.isInvalidToken(400, """
                {"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Request contains an invalid argument.",
                 "details":[{"@type":"type.googleapis.com/google.rpc.BadRequest","fieldViolations":[{"field":"message.token"}]}]}}"""));
        assertFalse(s.isInvalidToken(500, "not json"));
    }

    @Test
    void tokenEndpointFailureIsLoggedNotThrown() throws Exception {
        PushTokenRepository repo = mock(PushTokenRepository.class);
        List<PushToken> one = List.of(token(1, 7, "tok"));
        when(repo.findByMemberId(7L)).thenReturn(one);
        var p = new FcmProperties(serviceAccountJson(), base + "/missing-token-endpoint", base, null, 1000, 2000);
        var s = new FcmHttpV1PushSender(FcmServiceAccount.load(p.serviceAccountJson(), om), p, repo, om, Clock.systemUTC());
        assertDoesNotThrow(() -> s.send(7L, new PushMessage("t", "b", null)));
        assertEquals(0, requests.stream().filter(r -> r.path().startsWith("/v1/")).count());
    }

    @Test
    void serviceAccountLoadsFromFilePathAndBase64(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("fcm.json");
        Files.writeString(file, serviceAccountJson());
        assertEquals("test-proj", FcmServiceAccount.load(file.toString(), om).projectId());
        String b64 = Base64.getEncoder().encodeToString(serviceAccountJson().getBytes(StandardCharsets.UTF_8));
        FcmServiceAccount fromB64 = FcmServiceAccount.load(b64, om);
        assertEquals("fcm@test-proj.iam.gserviceaccount.com", fromB64.clientEmail());
        assertFalse(fromB64.toString().contains("PRIVATE"), "toString 에 키를 넣지 않는다");
        var e = assertThrows(IllegalStateException.class, () -> FcmServiceAccount.load(dir.resolve("nope.json").toString(), om));
        assertFalse(e.getMessage().contains("BEGIN"));
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withBean(PushTokenRepository.class, () -> mock(PushTokenRepository.class))
                .withConfiguration(UserConfigurations.of(PushSenderConfig.class));
    }

    @Test
    void loggingSenderSelectedWithoutCredentials() {
        runner().run(ctx -> assertThat(ctx).getBean(PushSender.class).isInstanceOf(LoggingPushSender.class));
        runner().withPropertyValues("push.fcm.service-account-json=")
                .run(ctx -> assertThat(ctx).getBean(PushSender.class).isInstanceOf(LoggingPushSender.class));
    }

    @Test
    void fcmSenderSelectedWhenCredentialFileConfigured(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("fcm.json");
        Files.writeString(file, serviceAccountJson());
        runner().withPropertyValues("push.fcm.service-account-json=" + file, "push.fcm.token-uri=" + base + "/token",
                        "push.fcm.api-base-url=" + base)
                .run(ctx -> {
                    PushSender s = ctx.getBean(PushSender.class);
                    assertThat(s).isInstanceOf(FcmHttpV1PushSender.class);
                    assertEquals(base + "/v1/projects/test-proj/messages:send", ReflectionTestUtils.getField(s, "sendUri"));
                });
        // 값이 있는데 읽을 수 없으면 기동 실패(조용히 dry-run 으로 떨어지지 않게)
        runner().withPropertyValues("push.fcm.service-account-json=" + dir.resolve("missing.json"))
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
