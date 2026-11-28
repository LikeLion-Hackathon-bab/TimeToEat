package com.example.timetoeat.infra.ai;

import com.example.timetoeat.domain.recommendation.dto.MenuRecommendationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 요청 본문은 Socket(menu.service.ts getMenuRecommendation)·기획 문서 §8.7 과 같은 계약:
 * { user_ids:[String], exclude_days, top_k }. 응답 recommendations[].{code,label,group_score,reasons},
 * per_user[].excluded_codes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRecommendGateway implements RecommendGateway {

    private final @Qualifier("aiWebClient") WebClient aiWebClient;
    private final AiProperties props;

    @Override
    public MenuRecommendationResponse recommendForMember(Long memberId, int excludeDays, int topK) {
        props.requireEnabled();
        JsonNode root = aiWebClient.post()
                .uri(props.getRecoBaseUrl() + "/v1/recommend")
                .header("X-AI-KEY", props.getOutboundKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "user_ids", List.of(String.valueOf(memberId)),
                        "exclude_days", excludeDays,
                        "top_k", topK))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return parse(root, topK);
    }

    static MenuRecommendationResponse parse(JsonNode root, int topK) {
        List<MenuRecommendationResponse.Item> items = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        if (root != null) {
            for (JsonNode r : root.path("recommendations")) {
                if (items.size() >= topK) break;
                String label = r.path("label").asText("");
                if (label.isBlank()) continue;
                List<String> reasons = new ArrayList<>();
                r.path("reasons").forEach(x -> reasons.add(x.asText()));
                JsonNode score = r.has("group_score") ? r.get("group_score") : r.get("score");
                items.add(new MenuRecommendationResponse.Item(
                        r.path("code").asText(null), label,
                        score == null || score.isNull() ? null : score.asDouble(), reasons));
            }
            for (JsonNode u : root.path("per_user")) {
                u.path("excluded_codes").forEach(c -> excluded.add(c.asText()));
            }
        }
        return new MenuRecommendationResponse(items, excluded);
    }
}
