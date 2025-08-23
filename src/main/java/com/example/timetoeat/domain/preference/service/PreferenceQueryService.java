package com.example.timetoeat.domain.preference.service;

import com.example.timetoeat.domain.preference.dto.response.PreferenceMetaResponse;
import com.example.timetoeat.domain.preference.dto.response.PreferenceSummaryResponse;
import com.example.timetoeat.domain.preference.entity.MemberFoodPreference;
import com.example.timetoeat.domain.preference.entity.PreferenceType;
import com.example.timetoeat.domain.preference.exception.PreferenceErrorCode;
import com.example.timetoeat.domain.preference.repository.MemberFoodPreferenceRepository;
import com.example.timetoeat.domain.preference.repository.MemberPreferenceProfileRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferenceQueryService {

    private final MemberFoodPreferenceRepository prefRepo;
    private final MemberPreferenceProfileRepository profileRepo;

    @Transactional(readOnly = true)
    public PreferenceSummaryResponse getMySummary(Long memberId) {
        if (memberId == null) throw new CustomException(PreferenceErrorCode.UNAUTHENTICATED);

        var likes = prefRepo.findByMember_IdAndType(memberId, PreferenceType.LIKE)
                .stream().map(this::toItem).toList();
        var dislikes = prefRepo.findByMember_IdAndType(memberId, PreferenceType.DISLIKE)
                .stream().map(this::toItem).toList();
        var allergies = prefRepo.findByMember_IdAndType(memberId, PreferenceType.ALLERGY)
                .stream().map(this::toItem).toList();

        return PreferenceSummaryResponse.of(likes, dislikes, allergies);
    }

    @Transactional(readOnly = true)
    public PreferenceMetaResponse getMyMeta(Long memberId) {
        if (memberId == null) throw new CustomException(PreferenceErrorCode.UNAUTHENTICATED);

        var profile = profileRepo.findById(memberId)
                .orElse(null);
        if (profile == null) {
            return PreferenceMetaResponse.of(null, null, 0L);
        }
        return PreferenceMetaResponse.of(
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getRevision()
        );
    }

    private PreferenceSummaryResponse.Item toItem(MemberFoodPreference p) {
        return PreferenceSummaryResponse.Item.builder()
                .code(p.getFoodCode().getCode())
                .label(p.getFoodCode().getLabel())
                .build();
    }
}
