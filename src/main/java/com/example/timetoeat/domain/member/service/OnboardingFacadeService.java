package com.example.timetoeat.domain.member.service;

import com.example.timetoeat.domain.member.dto.request.ProfileOnboardingRequest;
import com.example.timetoeat.domain.preference.dto.request.OnboardingPreferenceRequest;
import com.example.timetoeat.domain.preference.service.PreferenceCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingFacadeService {

    private final MemberProfileCommandService memberProfileCommandService;
    private final PreferenceCommandService preferenceCommandService;

    // 온보딩: 프로필 + 선호/비선호/알러지 저장(원자적 처리)
    public void onboard(Long meId, ProfileOnboardingRequest request) {

        // 1) 프로필 먼저 업데이트
        memberProfileCommandService.updateProfile(
                meId,
                request.getUserName(),
                request.getProfileImageUrl(),
                request.getBio()
        );

        // 2) 선호/비선호/알러지 저장(기존 온보딩 로직 재사용)
        var liked    = request.getLikedCodes()    == null ? List.<String>of() : request.getLikedCodes();
        var disliked = request.getDislikedCodes() == null ? List.<String>of() : request.getDislikedCodes();
        var allergy  = request.getAllergyCodes()  == null ? List.<String>of() : request.getAllergyCodes();

        preferenceCommandService.saveOnboarding(
                meId,
                OnboardingPreferenceRequest.of(
                        List.copyOf(liked),
                        List.copyOf(disliked),
                        List.copyOf(allergy)
                )
        );
    }
}
