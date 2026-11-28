package com.example.timetoeat.infra.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Google 서비스 계정 JSON 에서 FCM 발송에 필요한 값만 읽는다.
 * 오류 메시지에 JSON·키 내용을 넣지 않는다(로그 유출 방지).
 */
public record FcmServiceAccount(String projectId, String clientEmail, PrivateKey privateKey, String tokenUri) {

    public static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

    @Override
    public String toString() {
        return "FcmServiceAccount[projectId=" + projectId + ", clientEmail=" + clientEmail + "]";
    }

    /** 값은 ① 파일 경로(권장) ② base64 JSON ③ JSON 원문 중 하나. */
    public static FcmServiceAccount load(String value, ObjectMapper om) {
        return parse(resolveJson(value), om);
    }

    static String resolveJson(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) throw new IllegalStateException("FCM_SERVICE_ACCOUNT_JSON 이 비어 있습니다.");
        if (v.startsWith("{")) return v;
        try {
            Path p = Path.of(v);
            if (Files.isRegularFile(p)) return Files.readString(p, StandardCharsets.UTF_8);
        } catch (InvalidPathException ignored) {
            // base64 로 보고 아래에서 해석
        } catch (IOException e) {
            throw new IllegalStateException("FCM 서비스 계정 파일을 읽지 못했습니다: " + v, e);
        }
        try {
            String decoded = new String(Base64.getMimeDecoder().decode(v), StandardCharsets.UTF_8).trim();
            if (decoded.startsWith("{")) return decoded;
        } catch (IllegalArgumentException ignored) {
            // 아래 오류로
        }
        throw new IllegalStateException("FCM_SERVICE_ACCOUNT_JSON 은 서비스 계정 JSON 파일 경로 또는 base64 JSON 이어야 합니다(파일이 없거나 형식이 다름).");
    }

    static FcmServiceAccount parse(String json, ObjectMapper om) {
        JsonNode n;
        try {
            n = om.readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException("FCM 서비스 계정 JSON 을 해석하지 못했습니다.");
        }
        if (!"service_account".equals(n.path("type").asText())) {
            throw new IllegalStateException("FCM 자격 증명은 type=service_account JSON 이어야 합니다.");
        }
        String projectId = required(n, "project_id");
        String clientEmail = required(n, "client_email");
        PrivateKey key = privateKey(required(n, "private_key"));
        String tokenUri = n.path("token_uri").asText("");
        return new FcmServiceAccount(projectId, clientEmail, key, tokenUri.isBlank() ? DEFAULT_TOKEN_URI : tokenUri);
    }

    private static String required(JsonNode n, String field) {
        String v = n.path(field).asText("");
        if (v.isBlank()) throw new IllegalStateException("FCM 서비스 계정 JSON 에 " + field + " 가 없습니다.");
        return v;
    }

    private static PrivateKey privateKey(String pem) {
        try {
            String b64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new IllegalStateException("FCM 서비스 계정 private_key(PKCS#8 RSA)를 읽지 못했습니다.");
        }
    }
}
