package com.example.timetoeat.domain.article.listener;

import com.example.timetoeat.domain.article.dto.request.AiInferenceUpsertRequest;
import com.example.timetoeat.domain.article.event.ArticleCreatedEvent;
import com.example.timetoeat.domain.article.service.ArticleAiCommandService;
import com.example.timetoeat.infra.ai.AiGateway;
import com.example.timetoeat.infra.ai.retry.AiRetryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCreatedListener {

    private final AiGateway aiGateway;
    private final ArticleAiCommandService articleAiCommandService;
    private final Optional<AiRetryProducer> aiRetryProducer;

    // 트랜잭션 커밋 이후에만 실행 (DB 락 최소화)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(ArticleCreatedEvent e) {
        String imagePath = null;
        boolean tempDownloaded = false;

        try {
            // 1) 이미지 경로 결정 (URL이면 임시 파일로 저장)
            String imageUrl = e.getImageUrl();
            if (imageUrl != null) {
                try {
                    URI uri = URI.create(imageUrl);
                    String scheme = uri.getScheme();
                    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                        imagePath = downloadToTemp(imageUrl);
                        tempDownloaded = true;
                    } else {
                        imagePath = imageUrl; // 파일 경로/기타 스킴(URL의 맨 앞부분)
                    }
                } catch (IllegalArgumentException ex) {
                    imagePath = imageUrl; // URL 파싱 실패 -> 로컬 경로로 간주
                }
            }
            if (imagePath == null) {
                log.warn("No image to infer. articleId={}", e.getArticleId());
                return;
            }

            // 2) AI 동기 호출 → 업서트 저장
            var result = aiGateway.inferFoodSync(e.getArticleId(), e.getAuthorId(), imagePath, e.getMealAtKst());
            var upsert = AiInferenceUpsertRequest.from(result);
            articleAiCommandService.upsertMealLog(e.getArticleId(), upsert);

        } catch (Exception ex) {
            log.warn("AI inference post-commit failed. articleId={}, queue fallback if enabled", e.getArticleId(), ex);
            final String retryImagePath = imagePath;
            final LocalDateTime retryMealAtKst = e.getMealAtKst();
            aiRetryProducer.ifPresent(p -> p.sendInfer(e.getArticleId(), e.getAuthorId(), retryImagePath, retryMealAtKst));
        } finally {
            if (tempDownloaded && imagePath != null) {
                try {
                    Files.deleteIfExists(Path.of(imagePath));
                } catch (Exception ignore) {
                    log.debug("temp image delete failed: {}", imagePath);
                }
            }
        }
    }

    private String downloadToTemp(String url) throws Exception {
        Path temp = Files.createTempFile("article-img-", ".bin");
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return temp.toString();
    }
}
