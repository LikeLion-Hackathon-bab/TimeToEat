package com.example.timetoeat.infra.ai;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * food-ai(ultralytics 로더)는 파일 확장자로 형식을 고른다(.bmp .jpg .jpeg .png .tif .tiff .dng).
 * 내려받은 사진을 .bin 으로 저장하면 "No images or videos found ... .bin" 으로 500 이 났다.
 * 내용(매직 바이트)으로 형식을 판별해 맞는 확장자로 저장하고, WebP 처럼 food-ai 가 받지 않는 형식은 JPEG 로 바꾼다.
 *
 * 사진 주소는 사용자가 보낸다. 파일은 작아도 해상도가 아주 큰 사진(예: 16383x16383 WebP)을 디코딩하면
 * 메모리를 2GB 가까이 잡아 서버 전체가 멈춘다. 그래서 내려받기 크기·시간과 픽셀 수를 디코딩 전에 막는다.
 */
public final class AiImageFiles {
    private AiImageFiles() {}

    static final long MAX_BYTES = 15L * 1024 * 1024;
    static final long MAX_PIXELS = 25_000_000L;
    static final int CONNECT_TIMEOUT_MS = 3_000;
    static final int READ_TIMEOUT_MS = 5_000;

    /** 크기·해상도 제한에 걸린 사진. 다시 시도해도 같으니 재시도하지 않는다. */
    public static class RejectedImageException extends IOException {
        public RejectedImageException(String message) { super(message); }
    }

    /** http(s) 주소에서 내려받는다. 연결 3초, 읽기 5초, 15MB 까지. */
    public static Path fetch(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setInstanceFollowRedirects(false);
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("사진 내려받기 실패: HTTP " + code);
            long len = c.getContentLengthLong();
            if (len > MAX_BYTES) throw new RejectedImageException("사진이 너무 큼: " + len + " bytes");
            try (InputStream in = c.getInputStream()) {
                return download(in);
            }
        } finally {
            c.disconnect();
        }
    }

    /** 형식을 알 수 없거나 디코딩할 수 없으면(예: HEIC) IOException. 15MB·2500만 화소를 넘으면 RejectedImageException. */
    public static Path download(InputStream in) throws IOException {
        Path raw = Files.createTempFile("article-img-", ".tmp");
        try {
            copyLimited(in, raw);
            return normalize(raw);
        } finally {
            Files.deleteIfExists(raw);
        }
    }

    private static void copyLimited(InputStream in, Path out) throws IOException {
        try (var os = Files.newOutputStream(out)) {
            byte[] buf = new byte[64 * 1024];
            long total = 0;
            int r;
            while ((r = in.read(buf)) != -1) {
                total += r;
                if (total > MAX_BYTES) throw new RejectedImageException("사진이 너무 큼: 15MB 초과");
                os.write(buf, 0, r);
            }
        }
    }

    /** 헤더만 읽어 가로·세로를 확인한다. 읽을 수 있는 리더가 없으면 아무것도 하지 않는다. */
    static void checkPixels(Path raw) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(raw.toFile())) {
            if (iis == null) return;
            var readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                long w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w * h > MAX_PIXELS) {
                    throw new RejectedImageException("사진 해상도가 너무 큼: " + w + "x" + h);
                }
            } finally {
                reader.dispose();
            }
        }
    }

    /** raw 는 그대로 두고, food-ai 가 읽을 수 있는 확장자의 새 임시 파일을 돌려준다. */
    static Path normalize(Path raw) throws IOException {
        byte[] head = new byte[12];
        int n;
        try (InputStream is = Files.newInputStream(raw)) {
            n = is.readNBytes(head, 0, head.length);
        }
        checkPixels(raw); // food-ai 로 넘기는 형식도 같은 기준(그쪽 서버 메모리)
        String ext = nativeExtension(head, n);
        if (ext != null) {
            Path out = Files.createTempFile("article-img-", ext);
            Files.copy(raw, out, StandardCopyOption.REPLACE_EXISTING);
            return out;
        }
        BufferedImage img = ImageIO.read(raw.toFile()); // WebP 등(imageio-webp 플러그인)
        if (img == null) throw new IOException("food-ai가 읽을 수 없는 이미지 형식");
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(img, 0, 0, java.awt.Color.WHITE, null);
        Path out = Files.createTempFile("article-img-", ".jpg");
        if (!ImageIO.write(rgb, "jpg", out.toFile())) {
            Files.deleteIfExists(out);
            throw new IOException("JPEG 변환 실패");
        }
        return out;
    }

    static String nativeExtension(byte[] h, int n) {
        if (n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return ".jpg";
        if (n >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return ".png";
        if (n >= 2 && h[0] == 'B' && h[1] == 'M') return ".bmp";
        if (n >= 4 && ((h[0] == 'I' && h[1] == 'I' && h[2] == 42 && h[3] == 0)
                || (h[0] == 'M' && h[1] == 'M' && h[2] == 0 && h[3] == 42))) return ".tif";
        return null;
    }
}
