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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

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
    private final com.example.timetoeat.infra.ai.AiProperties aiProperties;
    private final com.example.timetoeat.infra.ai.ArticleImageFetcher imageFetcher;

    // 트랜잭션 커밋 이후에만 실행 (DB 락 최소화)
    // AFTER_COMMIT 시점에는 커밋이 끝난 원래 트랜잭션 자원이 아직 스레드에 묶여 있어,
    // 여기서 부르는 REQUIRED 서비스는 그 트랜잭션에 '참여'만 하고 쓰기가 커밋되지 않는다(F81: meal_log 0행).
    // NOT_SUPPORTED로 그 자원을 떼어 두면 AI 호출은 트랜잭션·DB 연결 없이 하고,
    // upsertMealLog(REQUIRED)는 자기 트랜잭션을 새로 열어 커밋한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void afterCommit(ArticleCreatedEvent e) {
        String imagePath = null;
        boolean tempDownloaded = false;
        if (!aiProperties.isEnabled()) { // AI_OUTBOUND_KEY 없음: 사진 추론을 건너뛴다(재시도 큐에도 넣지 않음)
            log.info("AI disabled (no AI_OUTBOUND_KEY), skip inference. articleId={}", e.getArticleId());
            return;
        }

        try {
            // 1) 사진 내려받기. https·허용 호스트·공인 IP 만, 시간·크기·해상도 제한(ArticleImageFetcher).
            //    로컬 파일 경로나 file: 주소를 그대로 food-ai 로 넘기던 동작은 없앴다.
            String imageUrl = e.getImageUrl();
            if (imageUrl != null) {
                imagePath = imageFetcher.fetch(imageUrl).toString();
                tempDownloaded = true;
            }
            if (imagePath == null) {
                log.warn("No image to infer. articleId={}", e.getArticleId());
                return;
            }

            // 2) AI 동기 호출 → 업서트 저장
            var result = aiGateway.inferFoodSync(e.getArticleId(), e.getAuthorId(), imagePath, e.getMealAtKst());
            var upsert = AiInferenceUpsertRequest.from(result);
            articleAiCommandService.upsertMealLog(e.getArticleId(), upsert);

        } catch (com.example.timetoeat.infra.ai.AiImageFiles.RejectedImageException ex) {
            // 다시 해도 같은 결과라 재시도 큐에 넣지 않는다.
            log.warn("AI inference skipped, image rejected. articleId={}, reason={}", e.getArticleId(), ex.getMessage());
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
}
