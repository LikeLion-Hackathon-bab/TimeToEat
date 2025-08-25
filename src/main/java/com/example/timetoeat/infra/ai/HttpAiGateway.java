package com.example.timetoeat.infra.ai;

import com.example.timetoeat.domain.article.dto.ai.InferenceResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpAiGateway implements AiGateway {

    private final @Qualifier("aiWebClient") WebClient aiWebClient;
    private final AiProperties props;

    @Override
    public InferenceResultDto inferFoodSync(Long articleId, Long authorId, String imagePath, LocalDateTime mealAtKst) {

        // KST -> OffsetDateTime(+09:00)
        OffsetDateTime mealAtOffset =
                mealAtKst.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("image", new FileSystemResource(imagePath));

        JsonNode root = aiWebClient.post()
                .uri(props.getFoodBaseUrl() + "/v1/infer")
                .header("X-AI-KEY", props.getOutboundKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(mb.build()))
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).flatMap(msg -> {
                            log.warn("AI infer failed: status={} url={} articleId={} authorId={} msg={}",
                                    resp.statusCode(), props.getFoodBaseUrl() + "/v1/infer", articleId, authorId, msg);
                            return reactor.core.publisher.Mono.error(new RuntimeException("AI infer failed: " + msg));
                        })
                )
                .bodyToMono(JsonNode.class)
                .block();

        var pick = selectBestDetection(root);

        // DTO로 변환 (timestamp=mealAtOffset)
        return InferenceResultDto.of(
                pick.code,
                pick.label,
                pick.confidence,
                mealAtOffset
        );
    }

    private static class Det {
        String code, label; double confidence;
        Det(String c, String l, double conf) { code = c; label = l; confidence = conf; }
    }

    private Det selectBestDetection(JsonNode root) {
        if (root == null) return new Det("00000000", "식별불가", 0.0);

        // decided
        JsonNode decided = root.get("decided");
        if (decided != null && !decided.isNull()) {
            return new Det(
                    decided.path("code").asText("00000000"),
                    decided.path("label").asText("식별불가"),
                    decided.path("confidence").asDouble(0.0)
            );
        }

        // candidates[0]
        JsonNode cands = root.get("candidates");
        if (cands != null && cands.isArray() && cands.size() > 0) {
            JsonNode c0 = cands.get(0);
            return new Det(
                    c0.path("code").asText("00000000"),
                    c0.path("label").asText("식별불가"),
                    c0.path("confidence").asDouble(0.0)
            );
        }

        // all_detections max confidence
        JsonNode all = root.get("all_detections");
        if (all != null && all.isArray() && all.size() > 0) {
            JsonNode best = all.get(0);
            for (JsonNode n : all) {
                if (n.path("confidence").asDouble(0.0) > best.path("confidence").asDouble(0.0)) best = n;
            }
            return new Det(
                    best.path("code").asText("00000000"),
                    best.path("label").asText("식별불가"),
                    best.path("confidence").asDouble(0.0)
            );
        }

        // 최상단에 바로 code/label/confidence가 오는 변형 대응
        if (root.has("code") || root.has("label")) {
            return new Det(
                    root.path("code").asText("00000000"),
                    root.path("label").asText("식별불가"),
                    root.path("confidence").asDouble(0.0)
            );
        }

        return new Det("00000000", "식별불가", 0.0);
    }

    @Deprecated
    public void requestInference(Long articleId, Long userId, String imageUrl,
                                 java.time.OffsetDateTime mealAtKst, String callbackUrl) {
        throw new UnsupportedOperationException("Use inferFoodSync(...) instead.");
    }
}
