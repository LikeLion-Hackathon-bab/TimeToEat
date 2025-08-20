package com.example.timetoeat.domain.posting.infrastructure.preference;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.preference.LoadPreferenceFood;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PreferenceFoodClient implements LoadPreferenceFood {
    private final RestTemplate restTemplate;

    @Value("${food.ai.base-url}")
    private String aiServerBaseUrl;

    @Override
    public List<String> loadFoodPreferences(List<String> imageUrls) {
        String inferUrl = aiServerBaseUrl + "/v1/infer";

        FoodAiRequest requestBody = new FoodAiRequest(imageUrls);

        try {
            // POST 요청으로 AI 서버 API 호출
            FoodAiResponse response = restTemplate.postForObject(inferUrl, requestBody, FoodAiResponse.class);

            if (response != null && response.results() != null) {
                return response.results();
            }
        } catch (Exception e) {
            System.err.println("AI 서버 호출 중 오류 발생: " + e.getMessage());
        }

        return Collections.emptyList();
    }

    private record FoodAiRequest(
            @JsonProperty("image_urls") List<String> imageUrls
    ) {}

    private record FoodAiResponse(
            List<String> results
    ) {}
}
