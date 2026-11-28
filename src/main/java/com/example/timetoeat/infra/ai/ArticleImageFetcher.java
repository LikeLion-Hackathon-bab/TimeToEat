package com.example.timetoeat.infra.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 게시물 사진 주소를 내려받아 food-ai 에 보낼 임시 파일로 만든다.
 *
 * 사진 주소는 사용자가 보낸 값이다. 전에는 아무 주소나 서버가 직접 열었다.
 * 그래서 내부 주소(예: http://food-ai:8000, 169.254.169.254)를 열거나, 로컬 파일 경로(/etc/hostname)를
 * 그대로 food-ai 로 보낼 수 있었다. 이제 https 주소만 받고, 허용 호스트 목록이 있으면 그 호스트만 받는다.
 * 호스트가 사설·루프백·링크 로컬 주소로 풀리면 막는다. 리다이렉트는 따라가지 않는다(AiImageFiles.fetch).
 */
@Component
public class ArticleImageFetcher {
    private final List<String> allowedHosts;

    public ArticleImageFetcher(@Value("${app.ai.image-allowed-hosts:}") String allowedHosts) {
        this.allowedHosts = allowedHosts == null ? List.of()
                : Arrays.stream(allowedHosts.split(",")).map(String::trim)
                        .map(h -> h.toLowerCase(Locale.ROOT)).filter(h -> !h.isEmpty()).toList();
    }

    /** 허용하지 않는 주소면 RejectedImageException. 다시 해도 같으니 재시도하지 않는다. */
    public Path fetch(String imageUrl) throws IOException {
        URI uri = checkAllowed(imageUrl);
        return AiImageFiles.fetch(uri.toString());
    }

    URI checkAllowed(String imageUrl) throws IOException {
        URI uri;
        try {
            uri = imageUrl == null ? null : URI.create(imageUrl.trim());
        } catch (IllegalArgumentException e) {
            uri = null;
        }
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new AiImageFiles.RejectedImageException("허용하지 않는 사진 주소(https 아님 또는 형식 오류)");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(host)) {
            throw new AiImageFiles.RejectedImageException("허용하지 않는 사진 호스트: " + host);
        }
        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new AiImageFiles.RejectedImageException("사진 호스트를 찾을 수 없음: " + host);
        }
        for (InetAddress a : addrs) {
            if (isInternal(a)) throw new AiImageFiles.RejectedImageException("내부 주소로 풀리는 사진 호스트: " + host);
        }
        return uri;
    }

    static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) return true;
        byte[] b = a.getAddress();
        if (a instanceof Inet6Address) {
            return (b[0] & 0xFE) == 0xFC; // fc00::/7 고유 로컬
        }
        int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF;
        return b0 == 0                                  // 0.0.0.0/8
                || (b0 == 100 && b1 >= 64 && b1 <= 127)  // 100.64.0.0/10 (CGNAT)
                || (b0 == 192 && b1 == 0 && (b[2] & 0xFF) == 0) // 192.0.0.0/24
                || (b0 == 198 && (b1 == 18 || b1 == 19)) // 198.18.0.0/15
                || b0 >= 240;                            // 240.0.0.0/4, 브로드캐스트
    }
}
