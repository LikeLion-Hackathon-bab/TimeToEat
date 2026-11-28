package com.example.timetoeat.infra.ai;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/** 사용자가 보낸 사진 주소로 내부 주소를 열거나 로컬 파일을 food-ai 로 보낼 수 없어야 한다. */
class ArticleImageFetcherTest {
    private final ArticleImageFetcher any = new ArticleImageFetcher("");
    private final ArticleImageFetcher cdn = new ArticleImageFetcher(" D1.cloudfront.net ,cdn.babmuckdang.site");

    private static void rejected(ArticleImageFetcher f, String url) {
        assertThrows(AiImageFiles.RejectedImageException.class, () -> f.checkAllowed(url), url);
    }

    @Test void localPathsAndOtherSchemesAreRejected() {
        for (String u : new String[]{"/etc/hostname", "uploads/a.jpg", "file:///etc/hostname", "ftp://x/a.jpg",
                "http://example.com/a.jpg", "jar:file:/a!/b", "", null, "https://user:pw@example.com/a.jpg"}) {
            rejected(any, u);
        }
    }

    @Test void internalAddressesAreRejected() {
        for (String u : new String[]{"https://127.0.0.1/a.jpg", "https://localhost/a.jpg", "https://169.254.169.254/latest/meta-data/",
                "https://10.0.0.5/a.jpg", "https://172.16.0.1/a.jpg", "https://192.168.0.10/a.jpg", "https://[::1]/a.jpg",
                "https://0.0.0.0/a.jpg", "https://100.64.0.1/a.jpg", "https://[fd00::1]/a.jpg", "https://[fe80::1]/a.jpg"}) {
            rejected(any, u);
        }
    }

    @Test void allowListOnlyAcceptsListedHosts() throws Exception {
        rejected(cdn, "https://evil.example/a.jpg");
        rejected(cdn, "https://d1.cloudfront.net.evil.example/a.jpg");
        rejected(cdn, "http://d1.cloudfront.net/a.jpg"); // https 만
    }

    @Test void publicAddressIsNotInternal() throws Exception {
        assertFalse(ArticleImageFetcher.isInternal(InetAddress.getByName("8.8.8.8")));
        assertFalse(ArticleImageFetcher.isInternal(InetAddress.getByName("13.124.0.1")));
        assertTrue(ArticleImageFetcher.isInternal(InetAddress.getByName("172.31.255.255")));
        assertTrue(ArticleImageFetcher.isInternal(InetAddress.getByName("192.0.0.8")));
    }

    @Test void redirectIsNotFollowed() throws Exception {
        // 302 로 내부 주소를 가리켜도 따라가지 않고 실패한다
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/a.jpg", ex -> {
            ex.getResponseHeaders().add("Location", "http://169.254.169.254/latest/meta-data/");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.start();
        try {
            var e = assertThrows(java.io.IOException.class,
                    () -> AiImageFiles.fetch("http://127.0.0.1:" + server.getAddress().getPort() + "/a.jpg"));
            assertTrue(e.getMessage().contains("302"), e.getMessage());
        } finally {
            server.stop(0);
        }
    }
}
