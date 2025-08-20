package com.example.timetoeat.domain.preference.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingPreferenceRequest {

    private List<String> likedCodes;
    private List<String> dislikedCodes;
    private List<String> allergyCodes;
}
