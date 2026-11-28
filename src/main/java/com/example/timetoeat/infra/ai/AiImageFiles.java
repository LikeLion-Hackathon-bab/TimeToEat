package com.example.timetoeat.infra.ai;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * food-ai(ultralytics 로더)는 파일 확장자로 형식을 고른다(.bmp .jpg .jpeg .png .tif .tiff .dng).
 * 내려받은 사진을 .bin 으로 저장하면 "No images or videos found ... .bin" 으로 500 이 났다.
 * 내용(매직 바이트)으로 형식을 판별해 맞는 확장자로 저장하고, WebP 처럼 food-ai 가 받지 않는 형식은 JPEG 로 바꾼다.
 */
public final class AiImageFiles {
    private AiImageFiles() {}

    /** 형식을 알 수 없거나 디코딩할 수 없으면(예: HEIC) IOException. */
    public static Path download(InputStream in) throws IOException {
        Path raw = Files.createTempFile("article-img-", ".tmp");
        try {
            Files.copy(in, raw, StandardCopyOption.REPLACE_EXISTING);
            return normalize(raw);
        } finally {
            Files.deleteIfExists(raw);
        }
    }

    /** raw 는 그대로 두고, food-ai 가 읽을 수 있는 확장자의 새 임시 파일을 돌려준다. */
    static Path normalize(Path raw) throws IOException {
        byte[] head = new byte[12];
        int n;
        try (InputStream is = Files.newInputStream(raw)) {
            n = is.readNBytes(head, 0, head.length);
        }
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
