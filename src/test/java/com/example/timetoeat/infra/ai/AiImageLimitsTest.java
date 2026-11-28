package com.example.timetoeat.infra.ai;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 사용자가 보낸 사진 주소로 서버 메모리를 다 쓰게 할 수 없어야 한다.
 * 파일은 작지만 해상도가 아주 큰 사진, 15MB 넘는 파일, 응답하지 않는 서버를 디코딩 전에 막는다.
 */
class AiImageLimitsTest {

    /** 헤더만 있는 PNG. 실제 픽셀 데이터 없이 가로·세로만 크다. */
    static byte[] pngHeader(int w, int h) {
        ByteBuffer ihdr = ByteBuffer.allocate(17);
        ihdr.put("IHDR".getBytes(StandardCharsets.US_ASCII)).putInt(w).putInt(h)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        ByteBuffer out = ByteBuffer.allocate(8 + 4 + 17 + 4);
        out.put(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        out.putInt(13).put(ihdr.array()).putInt((int) crc.getValue());
        return out.array();
    }

    /** VP8X 헤더만 있는 WebP(캔버스 크기만 큼). */
    static byte[] webpHeader(int w, int h) {
        ByteBuffer b = ByteBuffer.allocate(30).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(22).put("WEBP".getBytes(StandardCharsets.US_ASCII));
        b.put("VP8X".getBytes(StandardCharsets.US_ASCII)).putInt(10).put((byte) 0).put(new byte[3]);
        int cw = w - 1, ch = h - 1;
        b.put((byte) cw).put((byte) (cw >> 8)).put((byte) (cw >> 16));
        b.put((byte) ch).put((byte) (ch >> 8)).put((byte) (ch >> 16));
        return b.array();
    }

    @Test void hugeResolutionPngIsRejectedBeforeDecoding() {
        byte[] png = pngHeader(20_000, 20_000);
        assertTrue(png.length < 100);
        var ex = assertThrows(AiImageFiles.RejectedImageException.class,
                () -> AiImageFiles.download(new ByteArrayInputStream(png)));
        assertTrue(ex.getMessage().contains("20000x20000"), ex.getMessage());
    }

    @Test void hugeResolutionWebpIsRejectedBeforeDecoding() {
        byte[] webp = webpHeader(16_383, 16_383);
        var ex = assertThrows(AiImageFiles.RejectedImageException.class,
                () -> AiImageFiles.download(new ByteArrayInputStream(webp)));
        assertTrue(ex.getMessage().contains("16383x16383"), ex.getMessage());
    }

    @Test void normalPhotoSizeStillPasses() throws Exception {
        var img = new java.awt.image.BufferedImage(4000, 3000, java.awt.image.BufferedImage.TYPE_INT_RGB); // 1200만 화소
        var bos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", bos);
        assertTrue(AiImageFiles.download(new ByteArrayInputStream(bos.toByteArray())).toString().endsWith(".jpg"));
    }

    @Test void moreThan15MbIsRejectedWhileCopying() {
        InputStream endless = new InputStream() {
            long n;
            @Override public int read() { return n++ < 20L * 1024 * 1024 ? 0 : -1; }
            @Override public int read(byte[] b, int off, int len) {
                if (n >= 20L * 1024 * 1024) return -1;
                n += len; return len;
            }
        };
        assertThrows(AiImageFiles.RejectedImageException.class, () -> AiImageFiles.download(endless));
    }

    @Test void serverThatNeverAnswersTimesOut() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) { // 연결은 받지만 아무것도 보내지 않는다
            long t0 = System.nanoTime();
            assertThrows(java.io.IOException.class,
                    () -> AiImageFiles.fetch("http://127.0.0.1:" + silent.getLocalPort() + "/a.jpg"));
            long ms = (System.nanoTime() - t0) / 1_000_000;
            assertTrue(ms < AiImageFiles.READ_TIMEOUT_MS + 3_000, "걸린 시간 " + ms + "ms");
        }
    }
}
