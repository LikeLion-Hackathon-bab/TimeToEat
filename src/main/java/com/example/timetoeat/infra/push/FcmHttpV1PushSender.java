package com.example.timetoeat.infra.push;

import com.example.timetoeat.domain.push.entity.PushToken;
import com.example.timetoeat.domain.push.repository.PushTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FCM HTTP v1 발송기(https://firebase.google.com/docs/cloud-messaging/send-message).
 * 서비스 계정 키로 RS256 JWT 를 서명해 OAuth2 access token 을 받고(만료 1분 전까지 재사용),
 * 수신자의 등록 토큰마다 messages:send 를 호출한다.
 * 404 UNREGISTERED·400 INVALID_ARGUMENT(토큰 무효)는 그 토큰 행을 지우고, 429·5xx 는 재시도 없이 로그만 남긴다
 * (알림은 이미 저장돼 있고 앱 알림함에서 보이므로 웹 푸시는 최선 노력 발송).
 */
@Slf4j
public class FcmHttpV1PushSender implements PushSender {

    static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    private final FcmServiceAccount account;
    private final PushTokenRepository tokens;
    private final RestClient http;
    private final ObjectMapper om;
    private final Clock clock;
    private final String tokenUri;
    private final String sendUri;
    private final String linkBaseUrl;

    private volatile CachedToken cached;

    private record CachedToken(String value, Instant refreshAfter) { }

    record Reply(int status, String body) { }

    public FcmHttpV1PushSender(FcmServiceAccount account, FcmProperties props, PushTokenRepository tokens,
                               ObjectMapper om, Clock clock) {
        this.account = account;
        this.tokens = tokens;
        this.om = om;
        this.clock = clock;
        this.tokenUri = props.tokenUri() == null || props.tokenUri().isBlank() ? account.tokenUri() : props.tokenUri();
        this.sendUri = stripSlash(props.apiBaseUrl()) + "/v1/projects/" + account.projectId() + "/messages:send";
        this.linkBaseUrl = props.linkBaseUrl() == null || props.linkBaseUrl().isBlank() ? null : stripSlash(props.linkBaseUrl());
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.connectTimeoutMillis()))
                .build());
        factory.setReadTimeout(Duration.ofMillis(props.readTimeoutMillis()));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(Long receiverId, PushMessage message) {
        List<PushToken> list = tokens.findByMemberId(receiverId);
        if (list.isEmpty()) return;
        String accessToken;
        try {
            accessToken = accessToken();
        } catch (RuntimeException e) {
            log.warn("FCM access token 발급 실패 receiver={} msg={}", receiverId, e.getMessage());
            return;
        }
        for (PushToken t : list) {
            try {
                sendOne(accessToken, t, message);
            } catch (RuntimeException e) {
                log.warn("FCM 발송 오류 receiver={} tokenId={} msg={}", receiverId, t.getId(), e.getMessage());
            }
        }
    }

    private void sendOne(String accessToken, PushToken t, PushMessage message) {
        String body = write(Map.of("message", buildMessage(t.getToken(), message)));
        Reply r = http.post().uri(sendUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((req, res) -> new Reply(res.getStatusCode().value(), read(res.getBody())));
        int s = r.status();
        if (s >= 200 && s < 300) {
            log.debug("FCM 발송 성공 member={} tokenId={}", t.getMemberId(), t.getId());
            return;
        }
        if (isInvalidToken(s, r.body())) {
            log.info("FCM 무효 토큰 삭제 member={} tokenId={} status={}", t.getMemberId(), t.getId(), s);
            tokens.findByToken(t.getToken()).ifPresent(tokens::delete);
            return;
        }
        if (s == 401) cached = null; // 다음 발송에서 새 access token
        log.warn("FCM 발송 실패 member={} tokenId={} status={} body={}", t.getMemberId(), t.getId(), s, abbreviate(r.body()));
    }

    Map<String, Object> buildMessage(String token, PushMessage m) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("token", token);
        Map<String, String> notification = new LinkedHashMap<>();
        if (m.title() != null) notification.put("title", m.title());
        if (m.body() != null) notification.put("body", m.body());
        if (!notification.isEmpty()) msg.put("notification", notification);
        if (m.url() != null && !m.url().isBlank()) {
            msg.put("data", Map.of("url", m.url()));
            String link = absoluteLink(m.url());
            // fcm_options.link 는 HTTPS 절대 주소만 받는다(아니면 400). 상대 경로는 data.url 로만 보낸다.
            if (link != null) msg.put("webpush", Map.of("fcm_options", Map.of("link", link)));
        }
        return msg;
    }

    private String absoluteLink(String url) {
        if (url.startsWith("https://")) return url;
        if (linkBaseUrl != null && linkBaseUrl.startsWith("https://") && url.startsWith("/")) return linkBaseUrl + url;
        return null;
    }

    /**
     * 토큰을 지울 응답인지. 404 는 FcmError UNREGISTERED 일 때만(잘못된 project_id 404 로 토큰을 다 지우지 않게),
     * 400 INVALID_ARGUMENT 는 토큰 필드·등록 토큰 문제일 때만(본문 형식 오류로 정상 토큰을 지우지 않게).
     */
    boolean isInvalidToken(int status, String body) {
        JsonNode err;
        try {
            err = om.readTree(body == null || body.isBlank() ? "{}" : body).path("error");
        } catch (IOException e) {
            return false;
        }
        boolean unregistered = false;
        boolean tokenField = false;
        for (JsonNode d : err.path("details")) {
            if ("UNREGISTERED".equals(d.path("errorCode").asText())) unregistered = true;
            for (JsonNode v : d.path("fieldViolations")) {
                if ("message.token".equals(v.path("field").asText())) tokenField = true;
            }
        }
        if (status == 404) return unregistered;
        if (status == 400 && "INVALID_ARGUMENT".equals(err.path("status").asText())) {
            String msg = err.path("message").asText("").toLowerCase(Locale.ROOT);
            return tokenField || msg.contains("registration token");
        }
        return false;
    }

    String accessToken() {
        CachedToken c = cached;
        Instant now = clock.instant();
        if (c != null && now.isBefore(c.refreshAfter())) return c.value();
        synchronized (this) {
            c = cached;
            if (c != null && now.isBefore(c.refreshAfter())) return c.value();
            cached = c = fetchAccessToken(now);
            return c.value();
        }
    }

    @SuppressWarnings("deprecation")
    private CachedToken fetchAccessToken(Instant now) {
        String assertion = Jwts.builder()
                .header().add("typ", "JWT").and()
                .issuer(account.clientEmail())
                .claim("scope", SCOPE)
                // Google 토큰 엔드포인트는 aud 를 문자열로 받는다(jjwt 기본은 배열)
                .audience().single(tokenUri)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(account.privateKey(), Jwts.SIG.RS256)
                .compact();
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", JWT_BEARER);
        form.add("assertion", assertion);
        Reply r = http.post().uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((req, res) -> new Reply(res.getStatusCode().value(), read(res.getBody())));
        if (r.status() / 100 != 2) {
            throw new IllegalStateException("토큰 엔드포인트 응답 " + r.status() + " " + abbreviate(r.body()));
        }
        JsonNode n;
        try {
            n = om.readTree(r.body());
        } catch (IOException e) {
            throw new IllegalStateException("토큰 응답을 해석하지 못했습니다.");
        }
        String value = n.path("access_token").asText("");
        if (value.isBlank()) throw new IllegalStateException("토큰 응답에 access_token 이 없습니다.");
        long expiresIn = n.path("expires_in").asLong(3600);
        return new CachedToken(value, now.plusSeconds(Math.max(0, expiresIn - 60)));
    }

    private String write(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String read(java.io.InputStream in) throws IOException {
        return in == null ? "" : StreamUtils.copyToString(in, StandardCharsets.UTF_8);
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ");
        return one.length() > 300 ? one.substring(0, 300) + "…" : one;
    }

    private static String stripSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
