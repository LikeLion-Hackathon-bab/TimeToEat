package com.example.timetoeat.domain.member.service;


import com.example.timetoeat.domain.member.dto.response.ProfileDetailResponse;
import com.example.timetoeat.domain.member.dto.response.ProfileResponse;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.preference.entity.PreferenceType;
import com.example.timetoeat.domain.preference.repository.MemberFoodPreferenceRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileQueryService {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberFoodPreferenceRepository foodPreferenceRepository;

    public ProfileResponse getMyProfile(Long meId) {
        return getProfile(meId);
    }

    public ProfileResponse getProfile(Long memberId) {
        MemberEntity target = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        return ProfileResponse.from(target);
    }

    public ProfileDetailResponse getMyProfileDetail(Long meId) { return getProfileDetail(meId); }

    public ProfileDetailResponse getProfileDetail(Long memberId) {
        MemberEntity target = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        var likes = foodPreferenceRepository.findByMember_IdAndTypeOrderByFoodCode_LabelAsc(memberId, PreferenceType.LIKE).stream()
                .map(p -> new ProfileDetailResponse.PreferenceItem(p.getFoodCode().getCode(), p.getFoodCode().getLabel()))
                .toList();
        var dislikes = foodPreferenceRepository.findByMember_IdAndTypeOrderByFoodCode_LabelAsc(memberId, PreferenceType.DISLIKE).stream()
                .map(p -> new ProfileDetailResponse.PreferenceItem(p.getFoodCode().getCode(), p.getFoodCode().getLabel()))
                .toList();
        var allergies = foodPreferenceRepository.findByMember_IdAndTypeOrderByFoodCode_LabelAsc(memberId, PreferenceType.ALLERGY).stream()
                .map(p -> new ProfileDetailResponse.PreferenceItem(p.getFoodCode().getCode(), p.getFoodCode().getLabel()))
                .toList();

        return ProfileDetailResponse.of(target, likes, dislikes, allergies);
    }
}
