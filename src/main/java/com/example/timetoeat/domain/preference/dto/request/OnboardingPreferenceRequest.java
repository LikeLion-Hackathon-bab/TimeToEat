package com.example.timetoeat.domain.preference.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingPreferenceRequest {

    @Size(max = 200)
    private List<@Pattern(regexp = "\\d{8}", message = "코드는 8자리 숫자여야 합니다.") String> likedCodes;

    @Size(max = 200)
    private List<@Pattern(regexp = "\\d{8}", message = "코드는 8자리 숫자여야 합니다.") String> dislikedCodes;

    @Size(max = 200)
    private List<@Pattern(regexp = "\\d{8}", message = "코드는 8자리 숫자여야 합니다.") String> allergyCodes;

    /** 선택지에 없는 직접 입력 알레르기(F07). 공백 제거·중복 제거 뒤 최대 10개, 각 20자(서비스에서 검증). */
    @Size(max = 50)
    private List<String> customAllergyLabels;

    @Builder
    private OnboardingPreferenceRequest(List<String> likedCodes, List<String> dislikedCodes, List<String> allergyCodes,
                                        List<String> customAllergyLabels) {
        this.likedCodes = likedCodes;
        this.dislikedCodes = dislikedCodes;
        this.allergyCodes = allergyCodes;
        this.customAllergyLabels = customAllergyLabels;
    }

    public static OnboardingPreferenceRequest of(List<String> likedCodes, List<String> dislikedCodes, List<String> allergyCodes) {
        return OnboardingPreferenceRequest.builder()
                .likedCodes(likedCodes)
                .dislikedCodes(dislikedCodes)
                .allergyCodes(allergyCodes)
                .build();
    }

    public static OnboardingPreferenceRequest of(List<String> likedCodes, List<String> dislikedCodes, List<String> allergyCodes,
                                                 List<String> customAllergyLabels) {
        return OnboardingPreferenceRequest.builder()
                .likedCodes(likedCodes)
                .dislikedCodes(dislikedCodes)
                .allergyCodes(allergyCodes)
                .customAllergyLabels(customAllergyLabels)
                .build();
    }
}
