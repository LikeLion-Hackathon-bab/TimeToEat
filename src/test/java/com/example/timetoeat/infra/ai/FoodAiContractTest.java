package com.example.timetoeat.infra.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring → food-ai /v1/infer 계약. food-ai 는 multipart/form-data 의 user_id(문자열)·image(파일)를 필수로 받는다.
 * 1) 모의 서버: FastAPI 처럼 둘 중 하나라도 없으면 422 를 주고, Spring 이 둘 다 보내는지 확인.
 * 2) FOOD_AI_URL 이 있으면 실제 food-ai 컨테이너(gosua/food-ai:1.3.4)에 실제로 보내 본다.
 */
class FoodAiContractTest {

    private static Path image() throws Exception {
        Path p = Files.createTempFile("food", ".jpg");
        var img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(img, "jpg", p.toFile());
        return p;
    }

    private static HttpAiGateway gateway(String baseUrl) {
        return new HttpAiGateway(WebClient.builder().build(),
                new AiProperties(baseUrl, null, null, "qa-key", 10_000));
    }

    @Test
    void sendsUserIdAndImageAsMultipart() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> type = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/infer", ex -> {
            String b = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            body.set(b);
            type.set(ex.getRequestHeaders().getFirst("Content-Type"));
            boolean ok = b.contains("name=\"user_id\"") && b.contains("name=\"image\"");
            byte[] res = (ok
                    ? "{\"user_id\":\"7\",\"decided\":{\"code\":\"01011001\",\"label\":\"쌀밥\",\"confidence\":0.9},\"candidates\":[],\"all_detections\":[]}"
                    : "{\"detail\":[{\"type\":\"missing\",\"loc\":[\"body\",\"user_id\"]}]}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(ok ? 200 : 422, res.length);
            ex.getResponseBody().write(res);
            ex.close();
        });
        server.start();
        try {
            var r = gateway("http://127.0.0.1:" + server.getAddress().getPort())
                    .inferFoodSync(1L, 7L, image().toString(), LocalDateTime.of(2026, 12, 1, 12, 0));
            assertTrue(type.get().startsWith("multipart/form-data"), type.get());
            assertTrue(body.get().contains("name=\"user_id\"\r\n") || body.get().contains("name=\"user_id\""));
            assertTrue(body.get().contains("\r\n\r\n7\r\n"), "user_id 값은 작성자 id");
            assertEquals("쌀밥", r.getLabel());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FOOD_AI_URL", matches = "https?://.+")
    void realFoodAiContainerAcceptsTheRequest() throws Exception {
        // 실제 컨테이너는 사진에 음식이 없으면 decided=null·candidates=[] 로 200 을 준다 → 식별불가로 저장된다.
        var r = gateway(System.getenv("FOOD_AI_URL"))
                .inferFoodSync(1L, 7L, image().toString(), LocalDateTime.of(2026, 12, 1, 12, 0));
        assertNotNull(r.getLabel());
    }
}
