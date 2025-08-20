package com.example.timetoeat.domain.preference.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceSummaryResponse {

    private List<Item> likes;
    private List<Item> dislikes;
    private List<Item> allergies;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Item {
        private String code;
        private String label;

        @Builder
        private Item(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    public static PreferenceSummaryResponse of(List<Item> likes, List<Item> dislikes, List<Item> allergies) {

        PreferenceSummaryResponse response = new PreferenceSummaryResponse();
        response.likes = likes;
        response.dislikes = dislikes;
        response.allergies = allergies;

        return response;
    }
}
