package com.example.timetoeat.domain.preference.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
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
}
