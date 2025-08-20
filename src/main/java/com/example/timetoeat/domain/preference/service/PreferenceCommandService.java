package com.example.timetoeat.domain.preference.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.preference.dto.request.OnboardingPreferenceRequest;
import com.example.timetoeat.domain.preference.entity.FoodCode;
import com.example.timetoeat.domain.preference.entity.MemberFoodPreference;
import com.example.timetoeat.domain.preference.entity.MemberPreferenceProfile;
import com.example.timetoeat.domain.preference.entity.PreferenceType;
import com.example.timetoeat.domain.preference.exception.PreferenceErrorCode;
import com.example.timetoeat.domain.preference.repository.FoodCodeRepository;
import com.example.timetoeat.domain.preference.repository.MemberFoodPreferenceRepository;
import com.example.timetoeat.domain.preference.repository.MemberPreferenceProfileRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreferenceCommandService {

    private final MemberFoodPreferenceRepository prefRepo;
    private final FoodCodeRepository foodRepo;
    private final MemberPreferenceProfileRepository profileRepo;
    private final EntityManager em;

    @Transactional
    public void saveOnboarding(Long memberId, OnboardingPreferenceRequest req) {
        if (memberId == null) throw new CustomException(PreferenceErrorCode.UNAUTHENTICATED);

        List<String> likes = dedup(req.getLikedCodes());
        List<String> dislikes = dedup(req.getDislikedCodes());
        List<String> allergies = dedup(req.getAllergyCodes());


        // 같은 코드(음식)를 선호/비선호로 동시 선택 금지
        Set<String> overlap = new HashSet<>(likes);
        overlap.retainAll(dislikes);
        if (!overlap.isEmpty()) {
            throw new CustomException(PreferenceErrorCode.DUPLICATED_BETWEEN_LIKE_AND_DISLIKE);
        }

        // 존재하는 코드인지 검증
        Set<String> all = new HashSet<>();
        all.addAll(likes); all.addAll(dislikes); all.addAll(allergies);
        Map<String, FoodCode> codeMap = foodRepo.findAllById(all).stream()
                .collect(Collectors.toMap(FoodCode::getCode, f -> f));
        if (codeMap.size() != all.size()) {
            throw new CustomException(PreferenceErrorCode.CODE_NOT_FOUND);
        }

        MemberEntity memberRef = em.getReference(MemberEntity.class, memberId);
        replace(memberRef, PreferenceType.LIKE, likes, codeMap);
        replace(memberRef, PreferenceType.DISLIKE, dislikes, codeMap);
        replace(memberRef, PreferenceType.ALLERGY, allergies, codeMap);

        MemberPreferenceProfile profile = profileRepo.findById(memberId)
                .orElseGet(() -> profileRepo.save(MemberPreferenceProfile.create(memberRef)));
        profile.bumpRevision();
        profileRepo.save(profile);
    }

    private void replace(MemberEntity member, PreferenceType type, List<String> codes, Map<String, FoodCode> map) {
        prefRepo.deleteByMember_IdAndType(member.getId(), type);
        if (codes.isEmpty()) return;

        List<MemberFoodPreference> batch = new ArrayList<>(codes.size());
        for (String c : codes) {
            FoodCode food = map.get(c);
            if (food == null) throw new EntityNotFoundException("food_code not found: " + c);
            batch.add(MemberFoodPreference.builder()
                    .member(member)
                    .foodCode(food)
                    .type(type)
                    .build());
        }
        prefRepo.saveAll(batch);
    }

    private List<String> dedup(List<String> list) {
        if (CollectionUtils.isEmpty(list)) return List.of();
        return list.stream().filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).distinct().toList();
    }
}
