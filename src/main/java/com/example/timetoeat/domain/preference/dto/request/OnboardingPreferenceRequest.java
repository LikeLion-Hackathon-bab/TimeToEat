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

    @Builder
    private OnboardingPreferenceRequest(List<String> likedCodes, List<String> dislikedCodes, List<String> allergyCodes) {
        this.likedCodes = likedCodes;
        this.dislikedCodes = dislikedCodes;
        this.allergyCodes = allergyCodes;
    }

    public static OnboardingPreferenceRequest of(List<String> likedCodes, List<String> dislikedCodes, List<String> allergyCodes) {
        return OnboardingPreferenceRequest.builder()
                .likedCodes(likedCodes)
                .dislikedCodes(dislikedCodes)
                .allergyCodes(allergyCodes)
                .build();
    }
}
