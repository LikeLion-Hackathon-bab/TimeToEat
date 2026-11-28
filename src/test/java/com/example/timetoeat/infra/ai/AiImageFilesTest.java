package com.example.timetoeat.infra.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** 내려받은 게시물 사진을 food-ai 가 읽는 확장자로 저장한다(.bin → 500 이던 문제). */
class AiImageFilesTest {
    private static InputStream res(String name) {
        return AiImageFilesTest.class.getResourceAsStream("/ai/" + name);
    }

    @Test void jpegKeepsBytesWithJpgExtension() throws Exception {
        byte[] orig = res("rice.jpg").readAllBytes();
        Path p = AiImageFiles.download(new ByteArrayInputStream(orig));
        assertTrue(p.toString().endsWith(".jpg"));
        assertArrayEquals(orig, Files.readAllBytes(p));
    }

    @Test void pngGetsPngExtension() throws Exception {
        var img = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", bos);
        assertTrue(AiImageFiles.download(new ByteArrayInputStream(bos.toByteArray())).toString().endsWith(".png"));
    }

    @Test void webpIsConvertedToJpeg() throws Exception {
        Path p = AiImageFiles.download(res("rice.webp"));
        assertTrue(p.toString().endsWith(".jpg"));
        byte[] b = Files.readAllBytes(p);
        assertEquals(0xFF, b[0] & 0xFF);
        assertEquals(0xD8, b[1] & 0xFF);
        assertNotNull(javax.imageio.ImageIO.read(p.toFile()));
    }

    @Test void unknownFormatFails() {
        assertThrows(IOException.class, () -> AiImageFiles.download(new ByteArrayInputStream("not an image".getBytes())));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FOOD_AI_URL", matches = "https?://.+")
    void realFoodAiReadsConvertedWebp() throws Exception {
        Path p = AiImageFiles.download(res("rice.webp"));
        var gw = new HttpAiGateway(WebClient.builder().build(), new AiProperties(System.getenv("FOOD_AI_URL"), null, null, "k", 30_000));
        var r = gw.inferFoodSync(1L, 7L, p.toString(), LocalDateTime.of(2026, 12, 1, 12, 0));
        assertNotNull(r.getCode());
    }
}
